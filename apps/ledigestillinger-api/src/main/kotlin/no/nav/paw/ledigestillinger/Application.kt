package no.nav.paw.ledigestillinger

import no.nav.paw.health.HealthChecks
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.ledigestillinger.context.ApplicationContext

val appLogger = buildApplicationLogger

fun main() {
    appLogger.info("Starter Ledige Stillinger API...")
    val applicationContext = ApplicationContext()
    initEmbeddedKtorServer<HealthChecks>(applicationContext)
        .start(wait = true)
}
