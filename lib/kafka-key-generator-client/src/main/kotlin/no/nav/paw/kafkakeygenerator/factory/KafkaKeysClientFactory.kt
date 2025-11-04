package no.nav.paw.kafkakeygenerator.factory

import io.ktor.client.HttpClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.MockKafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.StandardKafkaKeysClient
import no.nav.paw.kafkakeygenerator.config.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaKeyConfig
import no.nav.paw.logging.logger.buildNamedLogger

private val logger = buildNamedLogger("no.nav.paw.kafkakeygenerator.factory")

fun createKafkaKeyGeneratorClient(
    httpClient: HttpClient = createHttpClient(),
    tokenClient: MachineToMachineTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment = currentRuntimeEnvironment,
        azureProviderConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    ),
    kafkaKeyConfig: KafkaKeyConfig = loadNaisOrLocalConfiguration(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)
): KafkaKeysClient {
    return if (kafkaKeyConfig.url.lowercase().startsWith("mock")) {
        logger.warn("Oppretter mock kafka-key-generator-client")
        mockKafkaKeysClient()
    } else {
        logger.info("Oppretter standard kafka-key-generator-client")
        standardKafkaKeysClient(
            httpClient = httpClient,
            kafkaKeyConfig = kafkaKeyConfig,
        ) {
            tokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
        }
    }
}

fun standardKafkaKeysClient(
    httpClient: HttpClient = createHttpClient(),
    kafkaKeyConfig: KafkaKeyConfig = loadNaisOrLocalConfiguration(KAFKA_KEY_GENERATOR_CLIENT_CONFIG),
    getAccessToken: () -> String
): KafkaKeysClient {
    return StandardKafkaKeysClient(
        httpClient = httpClient,
        baseUrl = kafkaKeyConfig.url,
        getAccessToken = getAccessToken
    )
}

fun mockKafkaKeysClient(): KafkaKeysClient {
    val naisClusterName = System.getenv("NAIS_CLUSTER_NAME")
    if (naisClusterName != null) {
        throw IllegalStateException("Kan ikke bruke MockKafkaKeysClient i $naisClusterName")
    }
    return MockKafkaKeysClient()
}
