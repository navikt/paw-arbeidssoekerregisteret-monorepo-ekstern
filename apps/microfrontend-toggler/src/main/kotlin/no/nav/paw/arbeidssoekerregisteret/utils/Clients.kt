package no.nav.paw.arbeidssoekerregisteret.utils

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}

fun KafkaKeysClient.getIdAndKeyOrNullBlocking(identitetsnummer: String): KafkaKeysResponse? = runBlocking {
    getIdAndKeyOrNull(identitetsnummer)
}
