package no.naw.paw.minestillinger.api.vo

import no.naw.paw.minestillinger.api.ApiStillingssoek

data class ApiBrukerprofil(
    val identitetsnummer: String,
    val tjenestestatus: ApiTjenesteStatus,
    val stillingssoek: List<ApiStillingssoek>,
)
