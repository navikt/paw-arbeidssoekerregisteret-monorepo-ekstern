package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.MetricsService
import no.nav.paw.database.plugin.FlywayMigrationCompleted
import no.nav.paw.scheduling.plugin.ScheduledTaskPlugin

fun Application.configureScheduledTask(
    applicationConfig: ApplicationConfig,
    metricsService: MetricsService
) {
    install(ScheduledTaskPlugin("PerioderMetrics")) {
        this.taskFunction = metricsService::tellAntallAktivePerioder
        this.delay = applicationConfig.perioderMetricsTaskDelay
        this.period = applicationConfig.perioderMetricsTaskInterval
        this.startEvent = FlywayMigrationCompleted
    }
}