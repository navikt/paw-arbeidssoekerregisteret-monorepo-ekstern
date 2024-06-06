package no.nav.paw.arbeidssoekerregisteret.config

import kotlinx.coroutines.runBlocking
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse? = runBlocking {
    getIdAndKey(identitetsnummer)
}

fun buildKafkaKeysClient(
    kafkaKeyConfig: KafkaKeyConfig, azureM2MTokenClient: AzureAdMachineToMachineTokenClient
) = kafkaKeysKlient(kafkaKeyConfig) {
    azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
}
