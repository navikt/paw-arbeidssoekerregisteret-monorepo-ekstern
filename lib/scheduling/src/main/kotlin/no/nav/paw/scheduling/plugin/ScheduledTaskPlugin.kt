package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.scheduling.function.defaultErrorFunction
import no.nav.paw.scheduling.function.defaultSuccessFunction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.scheduling")
private const val PLUGIN_NAME_SUFFIX = "ScheduledTaskPlugin"

class ScheduledTaskPluginConfig {
    var taskFunction: (() -> Unit)? = null
    var successFunction: (() -> Unit)? = null
    var errorFunction: ((throwable: Throwable) -> Unit)? = null
    var delay: Duration? = null
    var period: Duration? = null
    var startEvent: EventDefinition<Application>? = null
    var stopEvent: EventDefinition<Application>? = null
    val coroutineDispatcher: CoroutineDispatcher? = null
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(pluginInstance: Any): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val taskFunction = requireNotNull(pluginConfig.taskFunction) { "Task function er null" }
        val successFunction = pluginConfig.successFunction ?: ::defaultSuccessFunction
        val errorFunction = pluginConfig.errorFunction ?: ::defaultErrorFunction
        val delay = requireNotNull(pluginConfig.delay) { "Delay er null" }
        val period = requireNotNull(pluginConfig.period) { "Period er null" }
        val startEvent = pluginConfig.startEvent ?: ApplicationStarted
        val stopEvent = pluginConfig.stopEvent ?: ApplicationStopping
        val coroutineDispatcher = pluginConfig.coroutineDispatcher ?: Dispatchers.IO
        var job: Job? = null
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                try {
                    logger.info("Running scheduled task {}", pluginInstance)
                    taskFunction()
                    successFunction()
                } catch (throwable: Throwable) {
                    errorFunction(throwable)
                }
            }
        }

        on(MonitoringEvent(startEvent)) { application ->
            application.log.info("Scheduling task {} at delay: {}, period {}", pluginInstance, delay, period)
            job = application.launch(coroutineDispatcher) {
                timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
            }
        }

        on(MonitoringEvent(stopEvent)) { _ ->
            application.log.info("Canceling scheduled task {}", pluginInstance)
            timer.cancel()
            job?.cancel()
        }
    }
}
