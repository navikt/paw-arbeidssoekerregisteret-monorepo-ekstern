package no.naw.paw.ledigestillinger

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.health.HealthChecks
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.slf4j.LoggerFactory

val appLogger = LoggerFactory.getLogger("ledigie_stillinger_api")

fun main() {
    appLogger.info("Starter Ledigie Stillinger API...")
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG)
    val topics = listOf(
        HwmTopicConfig(
            topic = "teampam.stilling-ekstern-1",
            consumerVersion = APP_CONSUMER_VERSION,
            partitionCount = 2
        )
    )
    initDatabase(
        topics = topics,
        databaseConfig = loadNaisOrLocalConfiguration(DATABASE_CONFIG)
    )
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val healthChecks = HealthChecks()
    initEmbeddedKtorServer(
        prometheusRegistry = prometheusMeterRegistry,
        meterBinders = listOf(),
        healthIndicator = healthChecks,
        authProviders = securityConfig.authProviders,
    ).start(wait = true)
}
