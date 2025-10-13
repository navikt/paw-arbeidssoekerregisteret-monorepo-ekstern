package no.naw.paw.brukerprofiler.api

data class Brukerprofil(
    val identitetsnummer: String,
    val kanTilbysTjenestenLedigeStillinger: Boolean,
    val erTjenestenLedigeStillingerAktiv: Boolean,
    val stillingssoek: List<Stillingssoek>
)

sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

data class SimpeltSoek(
    override val soekType: StillingssoekType,
    val kommune: String,
    val styrk08: String
): Stillingssoek


enum class StillingssoekType {
    SIMPELT_SOEK_V1
}