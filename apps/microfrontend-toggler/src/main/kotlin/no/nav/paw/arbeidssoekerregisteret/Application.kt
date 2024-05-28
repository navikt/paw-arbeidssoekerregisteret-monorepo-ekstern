package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AZURE_M2M_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.KAFKA_KEY_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(APPLICATION_LOGGER_NAME)
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaKeyConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>(KAFKA_KEY_CONFIG_FILE_NAME)
    val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>(AZURE_M2M_CONFIG_FILE_NAME)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val azureM2MTokenClient = azureAdM2MTokenClient(appConfig.naisEnv, azureM2MConfig)
    val kafkaKeysClient = kafkaKeysKlient(kafkaKeyConfig) {
        azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
    }

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
        ConfigContext(appConfig, kafkaConfig).apply {
            LoggingContext(logger).apply {
                configureSerialization()
                configureRequestHandling()
                configureMetrics(meterRegistry)
                configureRouting(meterRegistry)
                configureKafka(meterRegistry, kafkaKeysClient)
            }
        }
    }
    server.addShutdownHook {
        server.stop(serverConfig.gracePeriodMillis, serverConfig.timeoutMillis)
        logger.info("Avslutter ${appConfig.appId}")
    }
    server.start(wait = true)
}