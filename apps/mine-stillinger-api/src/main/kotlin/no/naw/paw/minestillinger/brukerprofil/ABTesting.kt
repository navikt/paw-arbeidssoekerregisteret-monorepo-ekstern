package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.model.Identitetsnummer

fun sjekkABTestingGruppe(identitetsnummer: Identitetsnummer): Boolean {
    val andreSiffer = identitetsnummer.verdi[1].digitToInt()
    return andreSiffer % 2 == 0
}

