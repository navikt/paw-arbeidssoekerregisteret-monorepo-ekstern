package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ScheduledTaskService
import no.nav.paw.database.plugin.FlywayMigrationCompleted
import no.nav.paw.scheduling.plugin.ScheduledTaskPlugin

fun Application.configureScheduledTasks(
    applicationConfig: ApplicationConfig,
    scheduledTaskService: ScheduledTaskService
) {
    install(ScheduledTaskPlugin("PerioderVedlikehold")) {
        this.task = scheduledTaskService::perioderVedlikeholdTask
        this.delay = applicationConfig.perioderVedlikeholdTaskDelay
        this.period = applicationConfig.perioderVedlikeholdTaskInterval
        this.startEvent = FlywayMigrationCompleted
    }
    install(ScheduledTaskPlugin("PerioderMetrics")) {
        this.task = scheduledTaskService::perioderMetricsTask
        this.delay = applicationConfig.perioderMetricsTaskDelay
        this.period = applicationConfig.perioderMetricsTaskInterval
        this.startEvent = FlywayMigrationCompleted
    }
}