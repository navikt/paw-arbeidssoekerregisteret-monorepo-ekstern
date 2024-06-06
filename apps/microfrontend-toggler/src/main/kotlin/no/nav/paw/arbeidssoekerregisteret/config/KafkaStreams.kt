package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

context(LoggingContext)
fun buildStateListener(
    alivenessHealthIndicator: HealthIndicator,
    readinessHealthIndicator: HealthIndicator
) =
    KafkaStreams.StateListener { newState, _ ->
        when (newState) {
            KafkaStreams.State.CREATED -> {
                //alivenessHealthIndicator.setHealthy()
            }

            KafkaStreams.State.RUNNING -> {
                readinessHealthIndicator.setHealthy()
            }

            KafkaStreams.State.REBALANCING -> {
                readinessHealthIndicator.setHealthy()
            }

            KafkaStreams.State.PENDING_ERROR -> {
                readinessHealthIndicator.setUnhealthy()
            }

            KafkaStreams.State.PENDING_SHUTDOWN -> {
                readinessHealthIndicator.setUnhealthy()
            }

            KafkaStreams.State.ERROR -> {
                readinessHealthIndicator.setUnhealthy()
            }

            else -> {
                readinessHealthIndicator.setUnknown()
            }
        }

        logger.info("Kafka Streams aliveness er ${alivenessHealthIndicator.getStatus().value}")
        logger.info("Kafka Streams readyness er ${readinessHealthIndicator.getStatus().value}")
    }

context(LoggingContext)
fun buildUncaughtExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}
