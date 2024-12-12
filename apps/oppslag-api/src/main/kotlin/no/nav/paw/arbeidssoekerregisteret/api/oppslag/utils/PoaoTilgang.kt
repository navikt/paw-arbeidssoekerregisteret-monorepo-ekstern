package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.POAO_TILGANG_CLIENT_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.PoaoTilgangClientConfig
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import okhttp3.OkHttpClient
import java.time.Duration

fun createPoaoTilgangClient(runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment): PoaoTilgangClient {
    val poaoClientConfig = loadNaisOrLocalConfiguration<PoaoTilgangClientConfig>(POAO_TILGANG_CLIENT_CONFIG)
    val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(runtimeEnvironment, azureAdM2MConfig)
    return PoaoTilgangCachedClient(
        PoaoTilgangHttpClient(
            poaoClientConfig.url,
            { azureAdM2MTokenClient.createMachineToMachineToken(poaoClientConfig.scope) },
            OkHttpClient.Builder().callTimeout(Duration.ofSeconds(6))
                .addInterceptor(RetryInterceptor(maxRetries = 1)).build()
        )
    )
}