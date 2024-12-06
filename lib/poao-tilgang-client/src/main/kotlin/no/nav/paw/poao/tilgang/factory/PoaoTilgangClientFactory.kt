package no.nav.paw.poao.tilgang.factory

import io.ktor.client.HttpClient
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.poao.tilgang.client.PoaoTilgangClient
import no.nav.paw.poao.tilgang.config.POAO_TILGANG_CLIENT_CONFIG
import no.nav.paw.poao.tilgang.config.PoaoTilgangClientConfig

fun createPoaoTilgangClient(
    httpClient: HttpClient = createHttpClient(),
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): PoaoTilgangClient {
    val clientConfig = loadNaisOrLocalConfiguration<PoaoTilgangClientConfig>(POAO_TILGANG_CLIENT_CONFIG)
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    return PoaoTilgangClient(
        url = clientConfig.url,
        httpClient = httpClient
    ) {
        azureAdM2MTokenClient.createMachineToMachineToken(clientConfig.scope)
    }
}
