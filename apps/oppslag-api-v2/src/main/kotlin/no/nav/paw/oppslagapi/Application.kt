package no.nav.paw.oppslagapi

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
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

val appLogger = LoggerFactory.getLogger("app")

data class Configurations(
    val kafkaConfig: KafkaConfig = loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val topicNames: TopicNames
)

fun configurations(): Configurations = Configurations(
    kafkaConfig = loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG),
    securityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    topicNames = standardTopicNames(currentRuntimeEnvironment)
)

fun main() {
    appLogger.info("Starter oppslag-api-v2")
    val configurations = configurations()
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    initDatabase(configurations.topicNames)
    val webClients = initWebClients(httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson { registerKotlinModule() }
        }
    })
    val applicationQueryLogic = ApplicationQueryLogic(
        autorisasjonsTjeneste = AutorisasjonsTjeneste(
            tilgangsTjenesteForAnsatte = webClients.tilgangsTjenesteForAnsatte,
            kafkaKeysClient = webClients.kafkaKeysClient
        ),
        databaseQuerySupport = exposedDatabaseQuerySupport
    )
    val kafkaFactory = KafkaFactory(configurations.kafkaConfig)
    val consumer: Consumer<Long, ByteArray> = kafkaFactory.createConsumer(
        groupId = consumer_group,
        clientId = "oppslag-api-v2-${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    val rebalanceListener = HwmRebalanceListener(consumer_version, consumer)
    consumer.subscribe(configurations.topicNames.asList(), rebalanceListener)
    val deserializer: Deserializer<SpecificRecord> = kafkaFactory.kafkaAvroDeSerializer()
    val consumerMetrics = KafkaClientMetrics(consumer)
    val dataConsumerTask = DataConsumer(
        deserializer = deserializer,
        consumer = consumer,
        pollTimeout = Duration.ofMillis(1000L)
    )
    val healthIndicator = CompoudHealthIndicator(ExposedHealthIndicator, dataConsumerTask)
    dataConsumerTask.run()
    initEmbeddedKtorServer(
        prometheusRegistry = prometheusRegistry,
        meterBinders = listOf(consumerMetrics),
        healthIndicator = healthIndicator,
        authProviders = configurations.securityConfig.authProviders,
        appQueryLogic = applicationQueryLogic
    ).start(wait = true)
}
