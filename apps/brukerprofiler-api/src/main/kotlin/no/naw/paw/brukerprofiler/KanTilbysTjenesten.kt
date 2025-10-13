package no.naw.paw.brukerprofiler

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.Profilering
import no.naw.paw.brukerprofiler.domain.ProfileringResultat.ANTATT_GODE_MULIGHETER
import java.time.Duration.ofDays

val KAN_TILBYS_TJENESTEN_GYLDIGHETSPERIODE = ofDays(1)

fun kanTilbysTjenesten(
    brukerProfil: BrukerProfil?,
    profilering: Profilering?,
    harGradertAdresse: (Identitetsnummer) -> Boolean,
): Boolean {
    if (brukerProfil == null) return false
    if (!sjekkABTestingGruppe(brukerProfil.identitetsnummer)) return false
    val harBruktTjenesten = brukerProfil.harBruktTjenesten
    val harGodeMuligheter = profilering?.profileringResultat == ANTATT_GODE_MULIGHETER
    return when {
        !harBruktTjenesten && !harGodeMuligheter -> false
        else -> !harGradertAdresse(brukerProfil.identitetsnummer)
    }
}

fun sjekkABTestingGruppe(identitetsnummer: Identitetsnummer): Boolean {
    val andreSiffer = identitetsnummer.verdi[1].digitToInt()
    return andreSiffer % 2 == 0
}

