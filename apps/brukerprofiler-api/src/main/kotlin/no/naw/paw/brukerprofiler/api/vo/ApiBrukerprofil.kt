package no.naw.paw.brukerprofiler.api.vo

import no.naw.paw.brukerprofiler.api.ApiStillingssoek

data class ApiBrukerprofil(
    val identitetsnummer: String,
    val kanTilbysTjenestenLedigeStillinger: Boolean,
    val erTjenestenLedigeStillingerAktiv: Boolean,
    val stillingssoek: List<ApiStillingssoek>,
    val erIkkeInteressert: Boolean,
)
