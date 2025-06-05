package no.nav.paw.oppslagapi

import com.google.common.util.concurrent.UncaughtExceptionHandlers.systemExit
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.oppslagapi.kafka.HwmRebalanceListener
import no.nav.paw.oppslagapi.kafka.hwm.initHwm
import no.nav.paw.oppslagapi.kafka.hwm.updateHwm
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

const val consumer_version = 1
const val consumer_group = "oppslag-api-v2-consumer-v1"
const val partition_count = 6

val appLogger = LoggerFactory.getLogger("app")

fun main() {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dataSource = createHikariDataSource(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val topicNames = standardTopicNames
    Database.connect(dataSource)
    transaction {
        topicNames.asList().forEach { topic ->
            initHwm(topic, consumer_version, partition_count)
        }
    }
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG))
    val consumer: Consumer<Long, ByteArray> = kafkaFactory.createConsumer(
        groupId = consumer_group,
        clientId = "oppslag-api-v2-${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    val rebalanceListener = HwmRebalanceListener(consumer_version, consumer)
    consumer.subscribe(topicNames.asList(), rebalanceListener)
    val deserializer: Deserializer<SpecificRecord> = kafkaFactory.kafkaAvroDeSerializer()
    val dataConsumerTask = DataConsumer(
        deserializer = deserializer,
        consumer = consumer,
        pollTimeout = Duration.ofMillis(1000L)
    ).run().handle { _, throwable ->
        if (throwable != null) {
            appLogger.error("DataConsumer task failed", throwable)
            systemExit()
        } else {
            appLogger.info("DataConsumer task completed successfully")
        }
    }
    initKtor(
        prometheusRegistry = prometheusRegistry
    ).start(wait = true)
}

class DataConsumer(
    private val deserializer: Deserializer<SpecificRecord>,
    private val consumer: Consumer<Long, ByteArray>,
    private val pollTimeout: Duration = Duration.ofMillis(1000L)
) {
    private val sisteProessering = AtomicReference<Instant?>(null)
    private val erStartet = AtomicBoolean(false)

    fun run(): CompletableFuture<Void> {
        if (!erStartet.compareAndSet(false, true)) {
            throw IllegalStateException("DataConsumer kan kun startes en gang")
        }
        return runAsync {
            try {
                while (true) {
                    consumer.poll(pollTimeout)
                        .takeIf { !it.isEmpty }
                        ?.let { records ->
                            transaction {
                                records
                                    .asSequence()
                                    .filter { record ->
                                    updateHwm(
                                        consumerVersion = consumer_version,
                                        topic = record.topic(),
                                        partition = record.partition(),
                                        offset = record.offset()
                                    )
                                }.map { record -> record.toRow(deserializer) }
                                    .let { rows -> DataTable.batchInsert(
                                        data = rows,
                                        ignore = false,
                                        shouldReturnGeneratedValues = false
                                    ){ row ->
                                        this[DataTable.type] = row.type
                                        this[DataTable.identitetsnummer] = row.identitetsnummer
                                        this[DataTable.periodeId] = row.periodeId
                                        this[DataTable.timestamp] = row.timestamp
                                        this[DataTable.data] = row.data
                                    } }
                            }
                            sisteProessering.set(Instant.now())
                        }
                }
            } catch (e: InterruptedException) {
                appLogger.info("Consumer interrupted, shutting down gracefully")
            }
            runCatching { consumer.close(pollTimeout) }
        }
    }
}

fun ConsumerRecord<Long, ByteArray>.toRow(deserializer: Deserializer<SpecificRecord>): Row {
    when (val melding = deserializer.deserialize(topic(), this.value())) {
        is Periode -> {
            val (type, metadata) = if (melding.avsluttet == null) {
                periode_startet_v1 to melding.startet.toOpenApi()
            } else {
                periode_avsluttet_v1 to melding.avsluttet.toOpenApi()
            }
            return Row(
                identitetsnummer = melding.identitetsnummer,
                periodeId = melding.id,
                timestamp = metadata.tidspunkt,
                data = objectMapper.writeValueAsString(metadata),
                type = type
            )
        }
        is OpplysningerOmArbeidssoeker -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = opplysninger_om_arbeidssoeker_v4
            )
        }
        is Profilering -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = profilering_v1
            )
        }
        is Bekreftelse -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = melding.svar.sendtInnAv.tidspunkt,
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = bekreftelsemelding_v1
            )
        }
        is PaaVegneAv -> {
            return Row(
                identitetsnummer = null,
                periodeId = melding.periodeId,
                timestamp = Instant.ofEpochMilli(timestamp()),
                data = objectMapper.writeValueAsString(melding.toOpenApi()),
                type = pa_vegne_av_v1
            )
        }
        else -> throw IllegalArgumentException("Unsupported SpecificRecord type: ${this.javaClass}")
    }
}

const val periode_startet_v1 = "periode_startet-v1"
const val periode_avsluttet_v1 = "periode_avsluttet-v1"
const val opplysninger_om_arbeidssoeker_v4 = "opplysninger-v4"
const val profilering_v1 = "profilering-v1"
const val bekreftelsemelding_v1 = "bekreftelse-v1"
const val pa_vegne_av_v1 = "pa_vegne_av-v1"

data class Row(
    val type: String,
    val identitetsnummer: String?,
    val periodeId: UUID,
    val timestamp: Instant,
    val data: String
)