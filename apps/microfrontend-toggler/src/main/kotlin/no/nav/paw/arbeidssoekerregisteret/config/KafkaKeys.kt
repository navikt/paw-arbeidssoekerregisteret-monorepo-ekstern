package no.nav.paw.arbeidssoekerregisteret.config

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse? = runBlocking {
    getIdAndKey(identitetsnummer)
}