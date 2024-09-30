package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.plugins.configureTracing
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

fun main() {
    val logger = buildApplicationLogger

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val runtimeEnvironment = applicationConfig.runtimeEnvironment

    logger.info("Starter ${runtimeEnvironment.appNameOrDefaultForLocal()}")

    embeddedServer(
        factory = Netty,
        port = serverConfig.port,
        configure = {
            callGroupSize = serverConfig.callGroupSize
            workerGroupSize = serverConfig.workerGroupSize
            connectionGroupSize = serverConfig.connectionGroupSize
        }
    ) {
        module(applicationConfig)
    }.apply {
        addShutdownHook {
            stop(serverConfig.gracePeriodMillis, serverConfig.timeoutMillis)
            logger.info("Avslutter ${runtimeEnvironment.appNameOrDefaultForLocal()}")
        }
        start(wait = true)
    }
}

fun Application.module(applicationConfig: ApplicationConfig) {
    val applicationContext = ApplicationContext.create(applicationConfig)

    configureSerialization()
    configureRequestHandling()
    configureLogging()
    configureTracing()
    configureMetrics(applicationContext)
    configureKafka(applicationContext)
    configureAuthentication(applicationContext)
    configureRouting(applicationContext)
}
