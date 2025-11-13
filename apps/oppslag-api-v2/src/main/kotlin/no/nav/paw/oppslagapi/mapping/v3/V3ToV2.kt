package no.nav.paw.oppslagapi.mapping.v3

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.model.v2.V2BaseRequest
import no.nav.paw.oppslagapi.model.v2.V2IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v2.V2PerioderRequest
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerQueryRequest
import no.nav.paw.oppslagapi.model.v3.PerioderQueryRequest
import no.nav.paw.oppslagapi.model.v3.QueryRequest

fun IdentitetsnummerQueryRequest.asV2(): V2BaseRequest {
    return V2IdentitetsnummerRequest(Identitetsnummer(this.identitetsnummer))
}

fun PerioderQueryRequest.asV2(): V2BaseRequest {
    return V2PerioderRequest(this.perioder)
}

fun QueryRequest.asV2(): V2BaseRequest {
    return when (this) {
        is IdentitetsnummerQueryRequest -> {
            this.asV2()
        }

        is PerioderQueryRequest -> {
            this.asV2()
        }
    }
}
