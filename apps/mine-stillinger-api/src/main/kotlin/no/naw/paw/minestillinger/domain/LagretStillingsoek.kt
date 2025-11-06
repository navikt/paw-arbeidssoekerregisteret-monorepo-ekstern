package no.naw.paw.minestillinger.domain

import java.time.Instant

data class LagretStillingsoek(
    val id: SøkId,
    val brukerId: Long,
    val opprettet: Instant,
    val sistKjoet: Instant?,
    val soek: Stillingssoek
)