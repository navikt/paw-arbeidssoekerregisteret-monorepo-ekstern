package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom.FlywayMigrationCompleted
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom.ScheduledTaskPlugin
import java.time.Duration

fun Application.configureScheduledTask(
    task: (() -> Unit),
    delay: Duration = Duration.ofMillis(0),
    period: Duration = Duration.ofMinutes(10)
) {
    install(ScheduledTaskPlugin) {
        this.task = task
        this.delay = delay
        this.period = period
        this.startEvent = FlywayMigrationCompleted
    }
}