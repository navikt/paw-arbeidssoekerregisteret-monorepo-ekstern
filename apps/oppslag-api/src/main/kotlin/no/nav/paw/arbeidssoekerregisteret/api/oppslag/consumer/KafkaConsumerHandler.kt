package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildErrorLogger

class KafkaConsumerHandler() {
    private val errorLogger = buildErrorLogger

    fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer opplevde en uhåndterbar feil", throwable)
        throw throwable
    }
}