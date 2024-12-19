package no.nav.paw.pdl.factory

import io.ktor.client.HttpClient
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.config.PDL_CLIENT_CONFIG
import no.nav.paw.pdl.config.PdlClientConfig

fun createPdlClient(
    httpClient: HttpClient = createHttpClient(),
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): PdlClient {
    val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    return PdlClient(
        url = pdlClientConfig.url,
        tema = pdlClientConfig.tema,
        httpClient = httpClient
    ) { azureAdM2MTokenClient.createMachineToMachineToken(pdlClientConfig.scope) }
}
