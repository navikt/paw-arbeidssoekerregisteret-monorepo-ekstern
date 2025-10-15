package no.naw.paw.brukerprofiler.api.vo

import no.naw.paw.brukerprofiler.domain.Fylke
import no.naw.paw.brukerprofiler.kodeverk.SSBFylke
import no.naw.paw.brukerprofiler.kodeverk.SSBKommune

data class ApiFylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)

fun ApiFylke.domain() = Fylke(
    navn = navn,
    kommuner = kommuner,
)

fun populerFylkerMedKommuner(
    fylker: List<SSBFylke>,
    kommuner: List<SSBKommune>,
): List<ApiFylke> {
    val kommunerGruppertPåFylkesnummer = kommuner.groupBy { ssbKommune -> ssbKommune.fylkesnummer }

    return fylker.map { fylke ->
        val kommunerTilhørendeFylke: List<String> = kommunerGruppertPåFylkesnummer[fylke.fylkesnummer]
            .orEmpty()
            .map { it.nameList.first() } //TODO: kun første navn? Bør vi ta med alle?
        ApiFylke(
            kommuner = kommunerTilhørendeFylke,
            navn = fylke.nameList.firstOrNull() ?: fylke.fylkesnummer,
        )
    }
}