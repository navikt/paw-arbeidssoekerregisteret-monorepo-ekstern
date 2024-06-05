package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.HealthStatus
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

context(LoggingContext)
fun buildStateListener(healthIndicator: HealthIndicator) =
    KafkaStreams.StateListener { newState, _ ->
        when {
            newState.isRunningOrRebalancing -> {
                logger.debug("Kafka Streams endret helsetilstand til ${HealthStatus.HEALTHY.value}")
                healthIndicator.setHealthy()
            }

            newState.hasStartedOrFinishedShuttingDown() -> {
                logger.debug("Kafka Streams endret helsetilstand til ${HealthStatus.UNHEALTHY.value}")
                healthIndicator.setUnhealthy()
            }

            else -> {
                logger.debug("Kafka Streams endret helsetilstand til ${HealthStatus.UNKNOWN.value}")
                healthIndicator.setUnknown()
            }
        }
    }

context(LoggingContext)
fun buildUncaughtExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}
