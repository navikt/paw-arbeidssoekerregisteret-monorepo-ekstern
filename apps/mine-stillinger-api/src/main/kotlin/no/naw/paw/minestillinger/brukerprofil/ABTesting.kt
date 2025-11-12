package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.felles.model.Identitetsnummer

fun sjekkABTestingGruppe(regex: Regex, identitetsnummer: Identitetsnummer): Boolean {
    return regex.matches(identitetsnummer.value)
}

