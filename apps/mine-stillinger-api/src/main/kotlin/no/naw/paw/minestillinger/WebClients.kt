package no.naw.paw.minestillinger

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.factory.createPdlClient

class WebClients(
    val kafkaClient: KafkaKeysClient,
    val pdlClient: PdlClient,
    val finnStillingerClient: FinnStillingerClient
)

fun initWebClient(): WebClients {
    val m2mTokenClient = createAzureAdM2MTokenClient(
        currentRuntimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
    val finnStillingerClient = FinnStillingerClient(
        config =TODO("Config som faktisk er for TokenX exchange"),
        tokenProvider = TODO("TokenX exchange"),
        httpClient = httpClient
    )
    val kafkaKeyConfig: KafkaKeyConfig = loadNaisOrLocalConfiguration(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)
    val idClient = kafkaKeysClient(
        httpClient = httpClient,
        konfigurasjon = kafkaKeyConfig,
        m2mTokenFactory = { m2mTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope) }
    )
    val pdlClient = createPdlClient(
        httpClient = httpClient,
        azureAdM2MTokenClient = m2mTokenClient
    )
    return WebClients(
        kafkaClient = idClient,
        pdlClient = pdlClient,
        finnStillingerClient = finnStillingerClient
    )
}