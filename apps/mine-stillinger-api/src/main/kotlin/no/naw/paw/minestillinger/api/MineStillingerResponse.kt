package no.naw.paw.minestillinger.api

import java.time.Instant

data class MineStillingerResponse(
    val sistKjoert: Instant?,
    val soek: ApiStillingssoek,
    val resultat: List<ApiJobbAnnonse>
)