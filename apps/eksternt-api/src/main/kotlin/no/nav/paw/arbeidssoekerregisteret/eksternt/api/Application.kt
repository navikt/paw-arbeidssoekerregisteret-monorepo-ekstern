package no.nav.paw.arbeidssoekerregisteret.eksternt.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureDatabase
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureScheduledTasks
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal

private val logger = buildApplicationLogger

fun main() {
    val applicationContext = ApplicationContext.build()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext.serverConfig) {
        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(
            factory = Netty,
            host = host,
            port = port
        ) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    configureMetrics(
        applicationContext.meterRegistry,
        applicationContext.periodeKafkaConsumer
    )
    configureHTTP()
    configureAuthentication(applicationContext.securityConfig)
    configureLogging()
    configureSerialization()
    configureDatabase(applicationContext.dataSource)
    configureKafka(
        applicationContext.applicationConfig,
        applicationContext.periodeKafkaConsumer,
        applicationContext.periodeService
    )
    configureScheduledTasks(
        applicationContext.applicationConfig,
        applicationContext.scheduledTaskService
    )
    configureRouting(
        applicationContext.meterRegistry,
        applicationContext.periodeService
    )
}
