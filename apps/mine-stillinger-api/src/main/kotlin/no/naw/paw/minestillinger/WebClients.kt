package no.naw.paw.minestillinger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import no.nav.paw.security.texas.TEXAS_CONFIG
import no.nav.paw.security.texas.TexasClient
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient

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
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }
        }
    }
    val texasClient = TexasClient(
        httpClient = httpClient,
        config = loadNaisOrLocalConfiguration(TEXAS_CONFIG)
    )
    val finnStillingerClient = FinnStillingerClient(
        config =loadNaisOrLocalConfiguration("finn_ledige_stillinger.toml"),
        texasClient = texasClient,
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