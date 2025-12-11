package no.nav.paw.oppslagapi.routes.v2

import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.model.v2.V2BaseRequest
import no.nav.paw.oppslagapi.model.v2.V2IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v2.V2PerioderRequest
import no.nav.paw.security.authentication.model.SecurityContext

suspend fun ApplicationQueryLogic.hentTidslinjer(
    securityContext: SecurityContext,
    baseRequest: V2BaseRequest
) = when (baseRequest) {
    is V2IdentitetsnummerRequest -> hentTidslinjer(
        securityContext = securityContext,
        identitetsnummer = baseRequest.identitetsnummer
    )

    is V2PerioderRequest -> hentTidslinjer(
        securityContext = securityContext,
        perioder = baseRequest.perioder
    )
}