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
    return kafkaKeysClient(konfigurasjon = kafkaKeyConfig) {
        azureAdM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
    }
}

fun kafkaKeysClient(
    httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    },
    konfigurasjon: KafkaKeyConfig,
    m2mTokenFactory: () -> String
): KafkaKeysClient =
    when (konfigurasjon.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysMedHttpClient(
            httpClient = httpClient,
            config = konfigurasjon,
            m2mTokenFactory = m2mTokenFactory
        )
    }

fun kafkaKeysMedHttpClient(
    httpClient: HttpClient,
    config: KafkaKeyConfig,
    m2mTokenFactory: () -> String
): KafkaKeysClient {
    return StandardKafkaKeysClient(
        httpClient = httpClient,
        kafkaKeysUrl = config.url,
        kafkaKeysInfoUrl = config.infoUrl,
    ) { m2mTokenFactory() }
}
