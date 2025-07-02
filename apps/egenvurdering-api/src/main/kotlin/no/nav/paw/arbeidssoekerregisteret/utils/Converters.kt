package no.nav.paw.arbeidssoekerregisteret.utils

import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as ApiEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Profilering as ApiProfilering
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil as ApiProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.client.api.oppslag.models.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringsResultat

fun List<ArbeidssoekerperiodeAggregertResponse>.findSisteProfilering(): ProfileringAggregertResponse? =
    this.firstOrNull()?.opplysningerOmArbeidssoeker?.maxByOrNull { it.sendtInnAv.tidspunkt }?.profilering

fun ArbeidssoekerperiodeAggregertResponse.findSisteOpplysningerOmArbeidssoeker(): OpplysningerOmArbeidssoekerAggregertResponse? =
    this.opplysningerOmArbeidssoeker?.maxByOrNull { it.sendtInnAv.tidspunkt }

fun List<ArbeidssoekerperiodeAggregertResponse>.isPeriodeAvsluttet(): Boolean =
    this.isNotEmpty() && this[0].avsluttet != null

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

fun ProfileringAggregertResponse.toApiProfilering(): ApiProfilering =
    ApiProfilering(
        profileringId = profileringId,
        profilertTil = profilertTil.toApiProfilertTil() ?: throw IllegalArgumentException("Ugyldig profilertTil: $profilertTil"),
    )

fun ProfileringsResultat.toApiProfilertTil(): ApiProfilertTil? {
    return when (this) {
        ProfileringsResultat.ANTATT_GODE_MULIGHETER -> ApiProfilertTil.ANTATT_GODE_MULIGHETER
        ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING -> ApiProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfileringsResultat.OPPGITT_HINDRINGER -> ApiProfilertTil.OPPGITT_HINDRINGER
        else -> null
    }
}