package no.nav.paw.arbeidssoekerregisteret.eksternt.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.migrateDatabase
import no.nav.paw.config.env.appNameOrDefaultForLocal
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val logger = buildApplicationLogger

fun main() {
    val applicationContext = ApplicationContext.build()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext) {
        logger.info("Starter $appName med hostname ${serverConfig.host} og port ${serverConfig.port}")

        embeddedServer(
            factory = Netty,
            host = serverConfig.host,
            port = serverConfig.port,
            configure = {
                callGroupSize = serverConfig.callGroupSize
                workerGroupSize = serverConfig.workerGroupSize
                connectionGroupSize = serverConfig.connectionGroupSize
            }) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(
                    gracePeriodMillis = serverConfig.gracePeriodMillis,
                    timeoutMillis = serverConfig.timeoutMillis
                )
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    // Clean database etter versjon v1
    // cleanDatabase(dependencies.dataSource)

    // Migrerer database
    migrateDatabase(applicationContext.dataSource)

    // Konfigurerer plugins
    configureMetrics(applicationContext.meterRegistry, applicationContext.periodeKafkaConsumer)
    configureHTTP()
    configureAuthentication(applicationContext.securityConfig)
    configureLogging()
    configureSerialization()
    configureRouting(applicationContext.meterRegistry, applicationContext.periodeService)

    // Sletter data eldre enn inneværende år pluss tre år en gang i døgnet
    thread {
        applicationContext.scheduleDeletionService.scheduleDatabaseDeletionTask()
    }

    // Periode consumer
    thread {
        try {
            applicationContext.periodeConsumer.start()
        } catch (e: Exception) {
            logger.error("Periode consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }
    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        applicationContext.aktivePerioderGaugeScheduler.scheduleGetAktivePerioderTask()
    }
}
