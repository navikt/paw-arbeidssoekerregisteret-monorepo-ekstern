package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom.ScheduledTaskPluginConfig.Companion.PLUGIN_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import java.time.Duration
import java.util.*

private val logger = buildApplicationLogger

@KtorDsl
class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var delay: Duration? = null
    var period: Duration? = null
    var startEvent: EventDefinition<Application>? = null

    companion object {
        const val PLUGIN_NAME = "ScheduledTaskPlugin"
    }
}

val ScheduledTaskPlugin: ApplicationPlugin<ScheduledTaskPluginConfig> =
    createApplicationPlugin(PLUGIN_NAME, ::ScheduledTaskPluginConfig) {
        application.log.info("Oppretter {}", PLUGIN_NAME)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val delay = requireNotNull(pluginConfig.delay) { "Delay er null" }
        val period = requireNotNull(pluginConfig.period) { "Period er null" }
        val startEvent = requireNotNull(pluginConfig.startEvent) { "Start event er null" }
        var job: Job? = null
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                logger.debug("Running scheduled task")
                task()
            }
        }

        on(MonitoringEvent(startEvent)) { application ->
            application.log.info("Scheduling task at delay: {}, period {}", delay, period)
            job = application.launch(Dispatchers.IO) {
                timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
            }
        }

        on(MonitoringEvent(ApplicationStopping)) { _ ->
            application.log.info("Canceling scheduled task")
            job?.cancel()
        }
    }
