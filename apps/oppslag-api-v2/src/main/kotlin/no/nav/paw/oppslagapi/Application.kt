package no.nav.paw.oppslagapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.oppslagapi.data.consumer.ConsumerHealthMetric
import no.nav.paw.oppslagapi.data.consumer.DataConsumer
import no.nav.paw.oppslagapi.data.consumer.kafka.HwmRebalanceListener
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.exposedDatabaseQuerySupport
import no.nav.paw.oppslagapi.health.CompoudHealthIndicator
import no.nav.paw.oppslagapi.health.ExposedHealthIndicator
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

const val consumer_version = 1
const val consumer_group = "oppslag-api-v2-consumer-v$consumer_version"
const val partition_count = 6

val consumer_poll_timeout = Duration.ofSeconds(1)!!

val appLogger = LoggerFactory.getLogger("app")!!

fun main() {
    appLogger.info("Starter oppslag-api-v2")
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG)
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    initDatabase(topicNames, loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val webClients = initWebClients(httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    })
    val applicationQueryLogic = ApplicationQueryLogic(
        autorisasjonsTjeneste = AutorisasjonsTjeneste(
            tilgangsTjenesteForAnsatte = webClients.tilgangsTjenesteForAnsatte,
            kafkaKeysClient = webClients.kafkaKeysClient,
            auditLogger = AuditLogger.getLogger()
        ),
        databaseQuerySupport = exposedDatabaseQuerySupport,
        kafkaKeysClient = webClients.kafkaKeysClient
    )
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val consumer: Consumer<Long, ByteArray> = kafkaFactory.createConsumer(
        groupId = consumer_group,
        clientId = "oppslag-api-v2-${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    val consumerHealthMetric = ConsumerHealthMetric(prometheusRegistry)
    val rebalanceListener = HwmRebalanceListener(consumer_version, consumer, consumerHealthMetric)
    consumer.subscribe(topicNames.asList(), rebalanceListener)
    val deserializer: Deserializer<SpecificRecord> = kafkaFactory.kafkaAvroDeSerializer()
    val consumerMetrics = KafkaClientMetrics(consumer)
    val dataConsumerTask = DataConsumer(
        deserializer = deserializer,
        consumer = consumer,
        pollTimeout = consumer_poll_timeout,
        consumerHealthMetric = consumerHealthMetric
    )
    val healthIndicator = CompoudHealthIndicator(ExposedHealthIndicator, dataConsumerTask)
    dataConsumerTask.run()
    initEmbeddedKtorServer(
        prometheusRegistry = prometheusRegistry,
        meterBinders = listOf(
            consumerMetrics,
            JvmMemoryMetrics(),
            JvmGcMetrics()
        ),
        healthIndicator = healthIndicator,
        authProviders = securityConfig.authProviders,
        appQueryLogic = applicationQueryLogic
    ).start(wait = true)
}
