package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import java.time.Duration

fun Application.installScheduledTaskPlugin(
    pluginInstance: Any,
    task: (() -> Unit),
    delay: Duration = Duration.ZERO,
    period: Duration,
    startEvent: EventDefinition<Application> = ApplicationStarted
) {
    install(ScheduledTaskPlugin(pluginInstance)) {
        this.task = task
        this.delay = delay
        this.period = period
        this.startEvent = startEvent
    }
}
