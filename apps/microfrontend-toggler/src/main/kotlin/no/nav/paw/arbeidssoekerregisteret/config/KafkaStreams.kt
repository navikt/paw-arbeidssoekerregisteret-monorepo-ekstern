package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

context(LoggingContext)
fun buildStateListener(healthIndicator: HealthIndicator) =
    KafkaStreams.StateListener { newState, _ ->
        when {
            newState.isRunningOrRebalancing -> healthIndicator.setHealthy()
            newState.hasStartedOrFinishedShuttingDown() -> healthIndicator.setUnhealthy()
            else -> healthIndicator.setUnknown()
        }
    }

context(LoggingContext)
fun buildUncaughtExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}
