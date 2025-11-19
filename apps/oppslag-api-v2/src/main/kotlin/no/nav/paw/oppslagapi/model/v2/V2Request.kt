package no.nav.paw.oppslagapi.model.v2

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.security.authentication.model.SecurityContext
import java.util.UUID

sealed interface V2BaseRequest
data class V2IdentitetsnummerRequest(
    val identitetsnummer: Identitetsnummer
) : V2BaseRequest

data class V2PerioderRequest(
    val perioder: List<UUID>
) : V2BaseRequest

data class V2Request(
    val identitetsnummer: String?,
    val perioder: List<UUID>?
) {
    init {
        if (identitetsnummer != null) {
            require(identitetsnummer.matches(Regex("^\\d{11}$"))) {
                "Ugyldig identitetsnummer"
            }
            require(perioder == null) { "Kan ikke sende både identitetsnummer og perioder" }
        } else {
            require(perioder != null) { "Må sende enten identitetsnummer eller perioder" }
        }
    }

    val typedRequest: V2BaseRequest
        get() = when {
            identitetsnummer != null -> V2IdentitetsnummerRequest(Identitetsnummer(identitetsnummer))
            perioder != null -> V2PerioderRequest(perioder)
            else -> throw IllegalStateException("Ugyldig state, feil i input validering")
        }
}

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