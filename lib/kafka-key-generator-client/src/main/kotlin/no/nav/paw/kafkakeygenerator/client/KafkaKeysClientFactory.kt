package no.nav.paw.kafkakeygenerator.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

fun createKafkaKeyGeneratorClient(
    azureAdM2MTokenClient: AzureAdMachineToMachineTokenClient = createAzureAdM2MTokenClient(
        currentRuntimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
): KafkaKeysClient {
    val kafkaKeyConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)
    return kafkaKeysClient(kafkaKeyConfig) {
        azureAdM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
    }
}

fun kafkaKeysClient(konfigurasjon: KafkaKeyConfig, m2mTokenFactory: () -> String): KafkaKeysClient =
    when (konfigurasjon.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysMedHttpClient(konfigurasjon, m2mTokenFactory)
    }

private fun kafkaKeysMedHttpClient(config: KafkaKeyConfig, m2mTokenFactory: () -> String): KafkaKeysClient {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }
    return StandardKafkaKeysClient(
        httpClient,
        config.url
    ) { m2mTokenFactory() }
}
