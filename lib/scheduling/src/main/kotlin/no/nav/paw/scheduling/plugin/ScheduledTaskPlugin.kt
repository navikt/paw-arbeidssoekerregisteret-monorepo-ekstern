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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.scheduling")
private const val PLUGIN_NAME_SUFFIX = "ScheduledTaskPlugin"

abstract class TaskScheduler {
    abstract fun start(application: Application, name: String)
    abstract fun cancel(application: Application, name: String)
}

class ThreadPoolTaskScheduler(
    private val execute: () -> Unit,
    private val delay: Duration,
    private val period: Duration,
    private val executorService: ExecutorService
) : TaskScheduler() {

    override fun start(application: Application, name: String) {
        TODO("Not yet implemented")
    }

    override fun cancel(application: Application, name: String) {
        TODO("Not yet implemented")
    }
}

class CoroutineTaskScheduler(
    private val execute: () -> Unit,
    private val delay: Duration,
    private val period: Duration,
    private val coroutineDispatcher: CoroutineDispatcher
) : TaskScheduler() {
    private var job: Job? = null
    private val timer = Timer()
    private val timerTask = object : TimerTask() {
        override fun run() {
            execute()
        }
    }

    override fun start(application: Application, name: String) {
        application.log.info("Scheduling task {} at delay: {}, period {}", name, delay, period)
        job = application.launch(coroutineDispatcher) {
            timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
        }
    }

    override fun cancel(application: Application, name: String) {
        application.log.info("Canceling scheduled task {}", name)
        timer.cancel()
        job?.cancel()
    }
}

abstract class ScheduledTaskConfig {
    var taskFunction: (() -> Unit)? = null
    var successFunction: (() -> Unit) = ::defaultSuccessFunction
    var errorFunction: ((throwable: Throwable) -> Unit) = ::defaultErrorFunction
    var delay: Duration = Duration.ZERO
    var period: Duration? = Duration.ofMinutes(5)
    var startEvent: EventDefinition<Application> = ApplicationStarted
    var stopEvent: EventDefinition<Application> = ApplicationStopping
}

class ThreadPoolScheduledTaskConfig : ScheduledTaskConfig() {
    val executorService: ExecutorService = Executors.newSingleThreadExecutor()
}

class CoroutineScheduledTaskConfig : ScheduledTaskConfig() {
    val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
}

class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var onSuccess: (() -> Unit) = ::defaultSuccessFunction
    var onError: ((throwable: Throwable) -> Unit) = ::defaultErrorFunction
    var delay: Duration = Duration.ZERO
    var period: Duration? = Duration.ofMinutes(5)
    var startEvent: EventDefinition<Application> = ApplicationStarted
    var stopEvent: EventDefinition<Application> = ApplicationStopping

    fun coroutine(config: CoroutineScheduledTaskConfig.() -> Unit) {

    }

    fun threadPool(config: ((ThreadPoolScheduledTaskConfig) -> Unit)) {
    }
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(pluginInstance: Any): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val task = requireNotNull(pluginConfig.task) { "Task function er null" }
        val onSuccess = pluginConfig.onSuccess
        val onError = pluginConfig.onError
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
                    task()
                    onSuccess()
                } catch (throwable: Throwable) {
                    onError(throwable)
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
