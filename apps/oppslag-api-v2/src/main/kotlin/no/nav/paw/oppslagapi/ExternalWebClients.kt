package no.nav.paw.oppslagapi

import io.ktor.client.HttpClient
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysMedHttpClient
import no.nav.paw.tilgangskontroll.client.TILGANGSKONTROLL_CLIENT_CONFIG
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.client.TilgangskontrollClientConfig
import no.nav.paw.tilgangskontroll.client.tilgangsTjenesteForAnsatte

class ExternalWebClients(
    val tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte,
    val kafkaKeysClient: KafkaKeysClient
)

fun initWebClients(httpClient: HttpClient): ExternalWebClients {
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment = currentRuntimeEnvironment,
        azureProviderConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    val kafkaKeysClient = with (loadNaisOrLocalConfiguration<KafkaKeyConfig>(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)) {
        kafkaKeysMedHttpClient(
            httpClient = httpClient,
            config = this,
            m2mTokenFactory = { azureAdM2MTokenClient.createMachineToMachineToken(scope) }
        )
    }
    val tilgangskontroll = with(loadNaisOrLocalConfiguration<TilgangskontrollClientConfig>(TILGANGSKONTROLL_CLIENT_CONFIG)) {
        tilgangsTjenesteForAnsatte(
            httpClient = httpClient,
            config = this,
            tokenProvider = { azureAdM2MTokenClient.createMachineToMachineToken(scope) }
        )
    }
    return ExternalWebClients(
        tilgangsTjenesteForAnsatte = tilgangskontroll,
        kafkaKeysClient = kafkaKeysClient
    )
}