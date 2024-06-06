package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.common.audit_log.log.AuditLoggerConstants.AUDIT_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.config.StandardHealthIndicator
import no.nav.paw.arbeidssoekerregisteret.config.buildKafkaKeysClient
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleKafkaProducer
import no.nav.paw.arbeidssoekerregisteret.config.getIdAndKeyBlocking
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.plugins.configureTracing
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(APPLICATION_LOGGER_NAME)
    val auditLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(APPLICATION_CONFIG_FILE_NAME)
    val alivenessHealthIndicator = StandardHealthIndicator()
    alivenessHealthIndicator.setHealthy()
    val readinessHealthIndicator = StandardHealthIndicator()
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT) // TODO Instrumentere med metering og tracing
    val azureM2MTokenClient = azureAdM2MTokenClient(appConfig.naisEnv, appConfig.azureM2M)
    val kafkaKeysClient = buildKafkaKeysClient(appConfig.kafkaKeysClient, azureM2MTokenClient)
    val toggleKafkaProducer = buildToggleKafkaProducer(appConfig.kafka, appConfig.kafkaProducer)
    val toggleService = ToggleService(kafkaKeysClient, toggleKafkaProducer)

    logger.info("Starter ${appConfig.appId}")

    val server = embeddedServer(
        factory = Netty,
        port = serverConfig.port,
        configure = {
            callGroupSize = serverConfig.callGroupSize
            workerGroupSize = serverConfig.workerGroupSize
            connectionGroupSize = serverConfig.connectionGroupSize
        }
    ) {
        with(ConfigContext(appConfig)) {
            with(LoggingContext(logger, auditLogger)) {
                val kafkaStreamsMetrics = configureKafka(
                    alivenessHealthIndicator,
                    readinessHealthIndicator,
                    meterRegistry,
                    kafkaKeysClient::getIdAndKeyBlocking
                )?.let { KafkaStreamsMetrics(it) }
                configureSerialization()
                configureRequestHandling()
                configureLogging()
                configureTracing()
                configureMetrics(meterRegistry, kafkaStreamsMetrics)
                configureAuthentication()
                configureRouting(alivenessHealthIndicator, readinessHealthIndicator, meterRegistry, toggleService)
            }
        }
    }
    server.addShutdownHook {
        server.stop(serverConfig.gracePeriodMillis, serverConfig.timeoutMillis)
        logger.info("Avslutter ${appConfig.appId}")
    }
    server.start(wait = true)
}