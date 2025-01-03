package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.plugins.configureTracing
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal

fun main() {
    val logger = buildApplicationLogger
    val applicationContext = ApplicationContext.create()

    with(applicationContext) {
        val appName = serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

        logger.info("Starter $appName med port $serverConfig.port")

        embeddedServer(
            factory = Netty,
            port = serverConfig.port,
            configure = {
                callGroupSize = serverConfig.callGroupSize
                workerGroupSize = serverConfig.workerGroupSize
                connectionGroupSize = serverConfig.connectionGroupSize
            }
        ) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                stop(serverConfig.gracePeriodMillis, serverConfig.timeoutMillis)
                logger.info("Avslutter $appName")
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    configureSerialization()
    configureHTTP()
    configureLogging()
    configureTracing()
    configureMetrics(applicationContext)
    configureKafka(applicationContext)
    configureAuthentication(applicationContext)
    configureRouting(applicationContext)
}
