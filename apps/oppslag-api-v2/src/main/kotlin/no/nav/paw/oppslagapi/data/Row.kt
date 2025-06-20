package no.nav.paw.oppslagapi.data

import java.time.Instant
import java.util.UUID

data class Row<A>(
    val type: String,
    val identitetsnummer: String?,
    val periodeId: UUID,
    val timestamp: Instant,
    val data: A
)