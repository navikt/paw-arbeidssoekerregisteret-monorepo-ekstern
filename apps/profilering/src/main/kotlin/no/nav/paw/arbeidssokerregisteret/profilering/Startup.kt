package no.nav.paw.arbeidssokerregisteret.profilering

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

fun main() {
    val avroSchemaInfo = getModuleInfo("avro-schema")
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter: {} => {}", currentRuntimeEnvironment.appNameOrDefaultForLocal(), avroSchemaInfo)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    registerAppInfoGauges(prometheusMeterRegistry, applicationConfig)
    val streamsConfig = KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.joiningStateStoreName),
                Serdes.UUID(),
                streamsConfig.createSpecificAvroSerde()
            )
        )

    val topology = applicationTopology(
        streamBuilder = streamsBuilder,
        personInfoTjeneste = PersonInfoTjeneste.create(),
        applicationConfiguration = applicationConfig,
        prometheusRegistry = prometheusMeterRegistry
    )

    val kafkaStreams = KafkaStreams(topology, streamsConfig.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: $throwable", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    logger.info("Applikasjon startet")
    initKtor(
        kafkaStreamsMetrics = KafkaStreamsMetrics(kafkaStreams),
        prometheusRegistry = prometheusMeterRegistry,
        kafkaStreams = kafkaStreams,
    ).start(true)
    logger.info("Applikasjon stoppet")
}

private fun registerAppInfoGauges(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applicationConfig: ApplicationConfiguration
) {
    prometheusMeterRegistry.registerMainAvroSchemaGauges()
    prometheusMeterRegistry.registerTopicVersionGauge(
        topicInfo(
            topic = applicationConfig.profileringTopic,
            messageType = Profilering.`SCHEMA$`.name,
            description = "Profilering av arbeidssøkere",
            topicOperation = TopicOperation.WRITE
        )
    )
    prometheusMeterRegistry.registerTopicVersionGauge(
        topicInfo(
            topic = applicationConfig.periodeTopic,
            messageType = Periode.`SCHEMA$`.name,
            description = "Arbeidssøkerperioder",
            topicOperation = TopicOperation.READ
        )
    )
    prometheusMeterRegistry.registerTopicVersionGauge(
        topicInfo(
            topic = applicationConfig.opplysningerTopic,
            messageType = OpplysningerOmArbeidssoeker.`SCHEMA$`.name,
            description = "Opplysninger om arbeidssøkere",
            topicOperation = TopicOperation.READ
        )
    )
}

