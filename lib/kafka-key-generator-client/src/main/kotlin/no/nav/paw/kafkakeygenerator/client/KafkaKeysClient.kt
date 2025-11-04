package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse

interface KafkaKeysClient {
    suspend fun getIdAndKey(identitetsnummer: String): KafkaKeysResponse =
        getIdAndKeyOrNull(identitetsnummer)
            ?: throw IllegalStateException("Kafka-key-client: Uventet feil mot server: http-status=404")

    suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse?

    suspend fun getIdentiteter(
        identitetsnummer: String,
        visKonflikter: Boolean = false,
        hentFraPdl: Boolean = false
    ): IdentiteterResponse

    @Deprecated("Denne vil bli fjernet", ReplaceWith("getIdentiteter(identitetsnummer)"))
    suspend fun getInfo(identitetsnummer: String): KafkaKeysInfoResponse?
}
