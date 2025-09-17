package no.nav.paw.arbeidssoekerregisteret.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.client.api.oppslag.models.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringsResultat
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as ApiEgenvurdering

fun ArbeidssoekerperiodeAggregertResponse.findSisteOpplysningerOmArbeidssoeker(): OpplysningerOmArbeidssoekerAggregertResponse? =
    this.opplysningerOmArbeidssoeker?.maxByOrNull { it.sendtInnAv.tidspunkt }

fun ProfileringsResultat.toProfilertTil(): ProfilertTil {
    return when (this) {
        ProfileringsResultat.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfileringsResultat.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
        else -> throw IllegalArgumentException("Ugyldig profilertTil: $this")
    }
}

fun ApiEgenvurdering.toProfilertTil(): ProfilertTil =
    when (this) {
        ApiEgenvurdering.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        ApiEgenvurdering.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ApiEgenvurdering.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
    }
