package no.nav.paw.meldeplikttjeneste.tilstand

import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val ansvarlig: Ansvarlig? = null,
    val regler: Regler? = null,
    val sisteInnsending: Instant? = null
)

@JvmRecord
data class PeriodeInfo(
    val id: UUID,
    val recordKey: Long
)

@JvmRecord
data class Ansvarlig(
    val namespace: String,
    val id: String
)

@JvmRecord
data class Regler(
    val interval: Duration,
    val gracePeriode: Duration
)


fun tomPeriode(key: Long, id: UUID) = InternTilstand(
    periode = PeriodeInfo(
        id = id,
        recordKey = key
    )
)
