package no.naw.paw.brukerprofiler.domain

import java.time.Instant

data class LagretStillingsoek(
    val id: Long,
    val brukerId: Long,
    val opprettet: Instant,
    val sistKjoet: Instant?,
    val soek: Stillingssoek
)