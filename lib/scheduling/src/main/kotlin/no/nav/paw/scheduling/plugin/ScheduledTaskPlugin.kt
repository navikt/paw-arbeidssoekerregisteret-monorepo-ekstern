package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.scheduling")
private const val PLUGIN_NAME_SUFFIX = "ScheduledTaskPlugin"

class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var delay: Duration? = null
    var period: Duration? = null
    var startEvent: EventDefinition<Application>? = null
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(pluginInstance: Any): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val delay = pluginConfig.delay ?: Duration.ZERO
        val period = requireNotNull(pluginConfig.period) { "Period er null" }
        val startEvent = pluginConfig.startEvent ?: ApplicationStarted
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                logger.info("Running scheduled task {}", pluginInstance)
                task()
            }
        }

        on(MonitoringEvent(startEvent)) { application ->
            application.log.info("Scheduling task {} at delay: {}, period {}", pluginInstance, delay, period)
            timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
        }
    }
}
