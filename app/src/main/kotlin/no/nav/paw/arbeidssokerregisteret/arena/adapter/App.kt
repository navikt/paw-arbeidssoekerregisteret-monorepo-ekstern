package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.ApplicationConfig
import no.nav.paw.arbeidssokerregisteret.arena.adapter.health.Health
import no.nav.paw.arbeidssokerregisteret.arena.adapter.health.initKtor
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.meterIdMap
import no.nav.paw.arbeidssokerregisteret.arena.v4.ArenaArbeidssokerregisterTilstand
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDBKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture.runAsync

val logger = LoggerFactory.getLogger("App")
fun main() {
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application.toml")
    val (applicationIdSuffix, topics) = applicationConfig

    val kafkaStreamsFactory = KafkaStreamsFactory(applicationIdSuffix, kafkaStreamsConfig)

    val periodeSerde = kafkaStreamsFactory.createSpecificAvroSerde<Periode>()
    val opplysningerOmArbeidssoekerSerde = kafkaStreamsFactory.createSpecificAvroSerde<OpplysningerOmArbeidssoeker>()
    val profileringSerde = kafkaStreamsFactory.createSpecificAvroSerde<Profilering>()
    val arenaArbeidssokerregisterTilstandSerde =
        kafkaStreamsFactory.createSpecificAvroSerde<ArenaArbeidssokerregisterTilstand>()
    val tempArenaArbeidssokerregisterTilstandSerde = kafkaStreamsFactory.createSpecificAvroSerde<ArenaArbeidssokerregisterTilstand>()

    val stateStoreName = "periodeStateStore"
    val builder = StreamsBuilder()
        .addStateStore(
            KeyValueStoreBuilder(
                RocksDBKeyValueBytesStoreSupplier(stateStoreName, false),
                Serdes.String(),
                tempArenaArbeidssokerregisterTilstandSerde,
                Time.SYSTEM
            )
        )
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val topology: Topology = topology(
        builder = builder,
        topics = topics,
        stateStoreName = stateStoreName,
        periodeSerde = periodeSerde,
        opplysningerOmArbeidssoekerSerde = opplysningerOmArbeidssoekerSerde,
        profileringSerde = profileringSerde,
        arenaArbeidssokerregisterTilstandSerde = arenaArbeidssokerregisterTilstandSerde,
        registry = registry
    )
    val kafkaStreams = KafkaStreams(topology, kafkaStreamsFactory.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    runAsync {
        while(true) {
            val currentPartitions = kafkaStreams.metadataForLocalThreads().flatMap { thread ->
                thread.activeTasks().map { it.taskId().partition() }
            }.toList()
            meterIdMap.filter { (partition, _) -> partition !in currentPartitions }
                .forEach { (partition, id) ->
                    registry.remove(id.first)
                    meterIdMap.remove(partition)
                }
            Thread.sleep(Duration.ofMinutes(5))
        }
    }

    kafkaStreams.start()
    kafkaStreams.use {
        val health = Health(kafkaStreams)
        initKtor(
            kafkaStreamsMetrics = KafkaStreamsMetrics(kafkaStreams),
            prometheusRegistry = registry,
            health = health
        ).start(wait = true)
    }
}

