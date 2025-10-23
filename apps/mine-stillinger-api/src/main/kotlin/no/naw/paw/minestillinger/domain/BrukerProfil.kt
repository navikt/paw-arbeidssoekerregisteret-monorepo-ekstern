package no.naw.paw.minestillinger.domain

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import java.time.Instant

data class BrukerProfilerUtenFlagg(
    val id: BrukerId,
    val identitetsnummer: Identitetsnummer,
    val arbeidssoekerperiodeId: PeriodeId,
    val arbeidssoekerperiodeAvsluttet: Instant?
)

fun BrukerProfilerUtenFlagg.medFlagg(flaggListe: FlaggListe): BrukerProfil {
    return BrukerProfil(
        id = id,
        identitetsnummer = identitetsnummer,
        arbeidssoekerperiodeId = arbeidssoekerperiodeId,
        arbeidssoekerperiodeAvsluttet = arbeidssoekerperiodeAvsluttet,
        flaggListe = flaggListe
    )
}

data class BrukerProfil(
    val id: BrukerId,
    val identitetsnummer: Identitetsnummer,
    val arbeidssoekerperiodeId: PeriodeId,
    val arbeidssoekerperiodeAvsluttet: Instant?,
    val flaggListe: FlaggListe
) {
    val harBruktTjenesten: Boolean = flaggListe[HarBruktTjenestenFlagg]?.verdi ?: false
    val optOut: Boolean = flaggListe[OptOutFlagg]?.verdi ?: false
    val harGradertAdresse: Boolean = flaggListe[HarGradertAdresseFlagg]?.verdi ?: false
    val tjenestenErAktiv: Boolean = flaggListe[TjenestenErAktivFlagg]?.verdi ?: false
    val harGodeMuligheter: Boolean = flaggListe[HarGodeMuligheterFlagg]?.verdi ?: false
    val erITestGruppen: Boolean = flaggListe[ErITestGruppenFlagg]?.verdi ?: false

    inline fun <reified A: FlaggVerdi> flagg(): FlaggVerdi? {
        return flaggListe.filterIsInstance<A>().firstOrNull()
    }
}


fun BrukerProfil.api(): ApiBrukerprofil {
    return ApiBrukerprofil(
        identitetsnummer = identitetsnummer.verdi,
        tjenestestatus = when {
            tjenestenErAktiv -> ApiTjenesteStatus.AKTIV
            optOut -> ApiTjenesteStatus.OPT_OUT
            harGradertAdresse -> ApiTjenesteStatus.KAN_IKKE_LEVERES
            erITestGruppen && harGodeMuligheter -> ApiTjenesteStatus.INAKTIV
            else -> ApiTjenesteStatus.INAKTIV
        },
        stillingssoek = emptyList()
    )
}
