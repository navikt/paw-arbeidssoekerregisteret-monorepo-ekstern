package no.nav.paw.arbeidssoekerregisteret.eksternt.api

import io.ktor.server.application.*
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.arbeidssoekerRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.logger
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.migrateDatabase
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    // Avhengigheter
    val dependencies = createDependencies()
    val environmentConfig = environment.config

    // Clean database etter versjon v1
    // cleanDatabase(dependencies.dataSource)

    // Migrerer database
    migrateDatabase(dependencies.dataSource)

    // Konfigurerer plugins
    configureMetrics(dependencies.registry, dependencies.consumer)
    configureHTTP()
    configureAuthentication(environmentConfig)
    configureLogging()
    configureSerialization()

    // Sletter data eldre enn inneværende år pluss tre år en gang i døgnet
    thread {
        dependencies.scheduleDeletionService.scheduleDatabaseDeletionTask()
    }

    // Periode consumer
    thread {
        try {
            dependencies.periodeConsumer.start()
        } catch (e: Exception) {
            logger.error("Periode consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }
    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        dependencies.aktivePerioderGaugeScheduler.scheduleGetAktivePerioderTask()
    }

    // Ruter
    routing {
        healthRoutes(dependencies.registry)
        swaggerRoutes()
        arbeidssoekerRoutes(dependencies.arbeidssoekerService)
    }
}
