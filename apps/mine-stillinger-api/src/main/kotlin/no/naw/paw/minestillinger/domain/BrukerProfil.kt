package no.naw.paw.minestillinger.domain

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import java.time.Instant

data class BrukerProfilerUtenFlagg(
    val id: BrukerId,
    val identitetsnummer: Identitetsnummer,
    val arbeidssoekerperiodeId: PeriodeId,
    val arbeidssoekerperiodeAvsluttet: Instant?
)

fun BrukerProfilerUtenFlagg.medFlagg(listeMedFlagg: ListeMedFlagg): BrukerProfil {
    return BrukerProfil(
        id = id,
        identitetsnummer = identitetsnummer,
        arbeidssoekerperiodeId = arbeidssoekerperiodeId,
        arbeidssoekerperiodeAvsluttet = arbeidssoekerperiodeAvsluttet,
        listeMedFlagg = listeMedFlagg
    )
}

data class BrukerProfil(
    val id: BrukerId,
    val identitetsnummer: Identitetsnummer,
    val arbeidssoekerperiodeId: PeriodeId,
    val arbeidssoekerperiodeAvsluttet: Instant?,
    val listeMedFlagg: ListeMedFlagg
) {
    val harBruktTjenesten: Boolean = listeMedFlagg[HarBruktTjenestenFlaggtype]?.verdi ?: false
    val optOut: Boolean = listeMedFlagg[OptOutFlaggtype]?.verdi ?: false
    val harGradertAdresse: Boolean = listeMedFlagg[HarGradertAdresseFlaggtype]?.verdi ?: false
    val tjenestenErAktiv: Boolean = listeMedFlagg[TjenestenErAktivFlaggtype]?.verdi ?: false
    val harGodeMuligheter: Boolean = listeMedFlagg[HarGodeMuligheterFlaggtype]?.verdi ?: false
    val erITestGruppen: Boolean = listeMedFlagg[ErITestGruppenFlaggtype]?.verdi ?: false

    inline fun <reified A: Flagg> flagg(): Flagg? {
        return listeMedFlagg.filterIsInstance<A>().firstOrNull()
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
