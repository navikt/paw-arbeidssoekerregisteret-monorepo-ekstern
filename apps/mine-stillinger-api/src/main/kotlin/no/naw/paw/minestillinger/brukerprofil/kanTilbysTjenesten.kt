package no.naw.paw.minestillinger.brukerprofil

import no.naw.paw.minestillinger.domain.BrukerProfil

fun kanTilbysTjenesten(brukerProfil: BrukerProfil): Boolean {
    if (brukerProfil.harGradertAdresse) return false
    if (!brukerProfil.erITestGruppen) return false
    if (!brukerProfil.harBruktTjenesten && !brukerProfil.harGodeMuligheter) return false
    return true
}
