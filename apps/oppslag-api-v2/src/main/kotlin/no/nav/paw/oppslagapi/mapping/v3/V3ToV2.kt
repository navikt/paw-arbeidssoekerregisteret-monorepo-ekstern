package no.nav.paw.oppslagapi.mapping.v3

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.V2BaseRequest
import no.nav.paw.oppslagapi.V2IdentitetsnummerRequest
import no.nav.paw.oppslagapi.V2PerioderRequest
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v3.ListQueryRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeListRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeRequest
import no.nav.paw.oppslagapi.model.v3.SingleQueryRequest

fun SingleQueryRequest.asV2Request(): V2BaseRequest {
    return when (this) {
        is IdentitetsnummerRequest -> {
            V2IdentitetsnummerRequest(Identitetsnummer(this.identitetsnummer))
        }

        is PeriodeRequest -> {
            V2PerioderRequest(listOf(this.periodeId)) // TODO Hmmm...
        }
    }
}

fun ListQueryRequest.asV2Request(): V2BaseRequest {
    return when (this) {
        is IdentitetsnummerRequest -> {
            V2IdentitetsnummerRequest(Identitetsnummer(this.identitetsnummer))
        }

        is PeriodeListRequest -> {
            V2PerioderRequest(this.perioder)
        }
    }
}