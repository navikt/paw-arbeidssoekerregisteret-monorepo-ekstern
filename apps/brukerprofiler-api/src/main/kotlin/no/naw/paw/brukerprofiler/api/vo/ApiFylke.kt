package no.naw.paw.brukerprofiler.api.vo

import no.naw.paw.brukerprofiler.kodeverk.SSBFylke
import no.naw.paw.brukerprofiler.kodeverk.SSBKommune

data class ApiFylke(
    val navn: String,
    val fylkesnummer: String,
    val kommuner: List<ApiKommune> = emptyList(),
)
fun populerFylkerMedKommuner(
    fylker: List<SSBFylke>,
    kommuner: List<SSBKommune>,
): List<ApiFylke> {
    val kommunerGruppertPåFylkesnummer = kommuner.groupBy { ssbKommune -> ssbKommune.fylkesnummer }

    return fylker.map { fylke ->
        val kommunerTilhørendeFylke: List<SSBKommune> = kommunerGruppertPåFylkesnummer[fylke.fylkesnummer]
            .orEmpty()
        ApiFylke(
            kommuner = kommunerTilhørendeFylke.map { kommune ->
                ApiKommune(
                    kommunenummer = kommune.kommunenummer,
                    navn = kommune.nameList.first(),
                )
            },
            navn = fylke.nameList.first(),
            fylkesnummer = fylke.fylkesnummer
        )
    }
}