package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient

fun buildPoaoTilgangClient(
    poaoClientConfig: ServiceClientConfig,
    azureM2MTokenClient: AzureAdMachineToMachineTokenClient
) = PoaoTilgangCachedClient(
    PoaoTilgangHttpClient(poaoClientConfig.url,
        { azureM2MTokenClient.createMachineToMachineToken(poaoClientConfig.scope) }
    )
)