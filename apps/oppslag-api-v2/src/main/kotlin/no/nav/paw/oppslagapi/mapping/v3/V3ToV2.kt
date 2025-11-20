package no.nav.paw.oppslagapi.mapping.v3

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.model.v2.V2BaseRequest
import no.nav.paw.oppslagapi.model.v2.V2IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v2.V2PerioderRequest
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeListRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeRequest
import no.nav.paw.oppslagapi.model.v3.QueryRequest

fun QueryRequest.asV2Request(): V2BaseRequest {
    return when (this) {
        is IdentitetsnummerRequest -> {
            V2IdentitetsnummerRequest(Identitetsnummer(this.identitetsnummer))
        }

        is PeriodeRequest -> {
            V2PerioderRequest(listOf(this.periodeId))
        }

        is PeriodeListRequest -> {
            V2PerioderRequest(this.perioder)
        }
    }
}
