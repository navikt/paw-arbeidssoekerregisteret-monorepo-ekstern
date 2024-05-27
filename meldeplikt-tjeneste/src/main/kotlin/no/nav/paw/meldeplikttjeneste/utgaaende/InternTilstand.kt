package no.nav.paw.meldeplikttjeneste.tilstand

import java.time.Instant

@JvmRecord
data class MeldepliktHendelse(
    val periode: PeriodeInfo,
    val sisteInnsending: Instant? = null,
    val status: MeldepliktStatus,
    val ansvarlig: Ansvarlig,
    val tid
)

enum class MeldepliktStatus {
    OK,
    INTERVAL_UTLOEPT,
    GRACEPERIODE_UTLOEPT
}

enum class Ansvarlig {
    REGISTERET,
    EKSTERNT
}