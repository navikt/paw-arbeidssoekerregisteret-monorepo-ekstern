package no.nav.paw.aareg.factory

import io.ktor.client.HttpClient
import no.nav.paw.aareg.client.AaregClient
import no.nav.paw.aareg.config.AAREG_CLIENT_CONFIG
import no.nav.paw.aareg.config.AaregClientConfig
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

fun createAaregClient(
    httpClient: HttpClient = createHttpClient(),
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): AaregClient {
    val clientConfig = loadNaisOrLocalConfiguration<AaregClientConfig>(AAREG_CLIENT_CONFIG)
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    return AaregClient(
        url = clientConfig.url,
        httpClient = httpClient
    ) { azureAdM2MTokenClient.createMachineToMachineToken(clientConfig.scope) }
}
