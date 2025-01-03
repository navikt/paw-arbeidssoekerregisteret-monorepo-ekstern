package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.custom

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildApplicationLogger
import java.time.Duration
import java.util.*

private val logger = buildApplicationLogger
private const val PLUGIN_NAME_SUFFIX = "ScheduledTaskPlugin"

@KtorDsl
class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var delay: Duration? = null
    var period: Duration? = null
    var startEvent: EventDefinition<Application>? = null
    var stopEvent: EventDefinition<Application>? = null
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(pluginInstance: Any): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val delay = requireNotNull(pluginConfig.delay) { "Delay er null" }
        val period = requireNotNull(pluginConfig.period) { "Period er null" }
        val startEvent = pluginConfig.startEvent ?: ApplicationStarted
        val stopEvent = pluginConfig.stopEvent ?: ApplicationStopping
        var job: Job? = null
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                logger.info("Running scheduled task {}", pluginInstance)
                task()
            }
        }

        on(MonitoringEvent(startEvent)) { application ->
            application.log.info("Scheduling task {} at delay: {}, period {}", pluginInstance, delay, period)
            job = application.launch(Dispatchers.IO) {
                timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
            }
        }

        on(MonitoringEvent(stopEvent)) { _ ->
            application.log.info("Canceling scheduled task {}", pluginInstance)
            job?.cancel()
        }
    }
}
