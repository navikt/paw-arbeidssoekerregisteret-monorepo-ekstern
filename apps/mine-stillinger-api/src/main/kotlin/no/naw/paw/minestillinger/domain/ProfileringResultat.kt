package no.naw.paw.minestillinger.domain

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil

enum class ProfileringResultat {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

fun ProfilertTil.interntFormat(): ProfileringResultat = when (this) {
    ProfilertTil.UKJENT_VERDI -> ProfileringResultat.UKJENT_VERDI
    ProfilertTil.UDEFINERT -> ProfileringResultat.UDEFINERT
    ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringResultat.ANTATT_GODE_MULIGHETER
    ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringResultat.ANTATT_BEHOV_FOR_VEILEDNING
    ProfilertTil.OPPGITT_HINDRINGER -> ProfileringResultat.OPPGITT_HINDRINGER
}