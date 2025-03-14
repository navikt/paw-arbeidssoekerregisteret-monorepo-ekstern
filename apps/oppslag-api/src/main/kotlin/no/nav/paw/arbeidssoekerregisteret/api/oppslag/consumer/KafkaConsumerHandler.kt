package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildErrorLogger
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator

class KafkaConsumerHandler(
    private val livenessIndicator: LivenessHealthIndicator,
    private val readinessIndicator: ReadinessHealthIndicator
) {
    private val errorLogger = buildErrorLogger

    fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer opplevde en uhåndterbar feil", throwable)
        livenessIndicator.setUnhealthy()
        readinessIndicator.setUnhealthy()
        throw throwable
    }
}