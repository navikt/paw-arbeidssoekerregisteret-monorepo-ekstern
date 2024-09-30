package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.config.buildKafkaKeysClient
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleKafkaProducer
import no.nav.paw.arbeidssoekerregisteret.config.getIdAndKeyBlocking
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.plugins.configureTracing
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient

fun main() {
    val logger = buildApplicationLogger
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(APPLICATION_CONFIG_FILE_NAME)
    val healthIndicatorService = HealthIndicatorService()
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val azureM2MTokenClient = azureAdM2MTokenClient(appConfig.naisEnv, appConfig.azureM2M)
    val kafkaKeysClient = buildKafkaKeysClient(appConfig.kafkaKeysClient, azureM2MTokenClient)
    val toggleKafkaProducer = buildToggleKafkaProducer(appConfig.kafka, appConfig.kafkaProducer)
    val toggleService = ToggleService(appConfig, kafkaKeysClient, toggleKafkaProducer)

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
        val kafkaStreamsMetrics = configureKafka(
            appConfig,
            healthIndicatorService,
            meterRegistry,
            kafkaKeysClient::getIdAndKeyBlocking
        )
        configureSerialization()
        configureRequestHandling()
        configureLogging()
        configureTracing()
        configureMetrics(meterRegistry, listOf(KafkaClientMetrics(toggleKafkaProducer)), kafkaStreamsMetrics)
        configureAuthentication(appConfig)
        configureRouting(healthIndicatorService, meterRegistry, toggleService)
    }
    server.addShutdownHook {
        server.stop(serverConfig.gracePeriodMillis, serverConfig.timeoutMillis)
        logger.info("Avslutter ${appConfig.appId}")
    }
    server.start(wait = true)
}