package no.nav.paw.arbeidssoekerregisteret.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as ApiEgenvurdering

fun String.toProfilertTil(): ProfilertTil {
    return when (this) {
        "ANTATT_GODE_MULIGHETER" -> ProfilertTil.ANTATT_GODE_MULIGHETER
        "ANTATT_BEHOV_FOR_VEILEDNING" -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        "OPPGITT_HINDRINGER" -> ProfilertTil.OPPGITT_HINDRINGER
        else -> throw IllegalArgumentException("Ugyldig profilertTil: $this")
    }
}

fun ApiEgenvurdering.toProfilertTil(): ProfilertTil =
    when (this) {
        ApiEgenvurdering.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        ApiEgenvurdering.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ApiEgenvurdering.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
    }
