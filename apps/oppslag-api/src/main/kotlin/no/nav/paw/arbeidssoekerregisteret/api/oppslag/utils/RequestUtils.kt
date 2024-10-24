package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.StatusException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.*

private val logger = buildApplicationLogger

suspend inline fun <reified T : Any> ApplicationCall.getRequestBody(): T =
    try {
        receive<T>()
    } catch (e: Exception) {
        throw StatusException(HttpStatusCode.BadRequest, "Kunne ikke deserialisere request-body")
    }

private fun ApplicationCall.getClaim(
    issuer: String,
    name: String
): String? =
    authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims(issuer)
        ?.getStringClaim(name)

fun ApplicationCall.getPidClaim(): Identitetsnummer =
    getClaim("tokenx", "pid")?.toIdentitetsnummer()
        ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'pid'-claim i token fra tokenx-issuer")

private fun ApplicationCall.getNavAnsattAzureId(): String = getClaim("azure", "oid") ?: throw StatusException(
    HttpStatusCode.Forbidden,
    "Fant ikke 'oid'-claim i token fra azure-issuer"
)

private fun ApplicationCall.getNavAnsattIdent(): String = getClaim("azure", "NAVident") ?: throw StatusException(
    HttpStatusCode.Forbidden,
    "Fant ikke 'NAVident'-claim i token fra azure-issuer"
)

private fun ApplicationCall.isMachineToMachineToken(): Boolean =
    authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("azure")
        ?.getAsList("roles")
        ?.contains("access_as_application")
        ?: false

fun ApplicationCall.getNavAnsattFromToken(): NavAnsatt? =
    if (this.isMachineToMachineToken()) {
        this.request.headers["Nav-Ident"]
            ?.let { navIdent ->
                NavAnsatt(
                    getNavAnsattAzureId(),
                    navIdent
                )
            }
    } else {
        NavAnsatt(
            getNavAnsattAzureId(),
            getNavAnsattIdent()
        )
    }

fun ApplicationCall.verifyAccessFromToken(
    authorizationService: AuthorizationService,
    identitetsnummerList: List<Identitetsnummer>,
) {
    if (identitetsnummerList.isEmpty()) {
        throw StatusException(HttpStatusCode.Forbidden, "Fant ingen identitetsnummer for sluttbruker")
    }

    val navAnsatt = getNavAnsattFromToken()

    if (navAnsatt != null) {
        authorizationService.verifiserTilgangTilBruker(navAnsatt, identitetsnummerList).let { harTilgang ->
            if (!harTilgang) {
                throw StatusException(HttpStatusCode.Forbidden, "Innlogget bruker mangler tilgang")
            }
        }
    } else {
        logger.debug("Access Token er et M2M-token")
    }
}

fun verifyPeriodeId(
    periodeId: UUID?,
    identitetsnummerList: List<Identitetsnummer>,
    periodeService: PeriodeService
) {
    if (identitetsnummerList.isEmpty()) {
        throw StatusException(HttpStatusCode.Forbidden, "Fant ingen identitetsnummer for sluttbruker")
    }

    if (periodeId != null) {

        val periode = periodeService.hentPeriodeForId(periodeId) ?: throw StatusException(
            HttpStatusCode.BadRequest,
            "Finner ikke periode $periodeId"
        )

        val identiteter = identitetsnummerList.map { it.verdi }
        if (!identiteter.contains(periode.identitetsnummer)) {
            throw StatusException(HttpStatusCode.Forbidden, "Periode $periodeId tilhører ikke sluttbruker")
        }
    }
}

fun createSisteSamletInformasjonResponse(
    identitetsnummerList: List<Identitetsnummer>,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService
): SamletInformasjonResponse {
    val arbeidssoekerperioder = periodeService.finnPerioderForIdentiteter(identitetsnummerList)
    val sistePeriode = arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }

    val sisteOpplysninger = sistePeriode?.let { periode ->
        opplysningerService
            .finnOpplysningerForPeriodeId(periode.periodeId).maxByOrNull { it.sendtInnAv.tidspunkt }
    }
    val sisteProfilering = sistePeriode?.let { periode ->
        profileringService
            .finnProfileringerForPeriodeId(periode.periodeId).maxByOrNull { it.sendtInnAv.tidspunkt }
    }

    return SamletInformasjonResponse(
        arbeidssoekerperioder = listOfNotNull(sistePeriode),
        opplysningerOmArbeidssoeker = listOfNotNull(sisteOpplysninger),
        profilering = listOfNotNull(sisteProfilering)
    )
}

fun createSamletInformasjonResponse(
    identitetsnummerList: List<Identitetsnummer>,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService
): SamletInformasjonResponse {
    val arbeidssoekerperioder = periodeService.finnPerioderForIdentiteter(identitetsnummerList)
    val opplysningerOmArbeidssoeker = opplysningerService.finnOpplysningerForIdentiteter(identitetsnummerList)
    val profilering = profileringService.finnProfileringerForIdentiteter(identitetsnummerList)

    return SamletInformasjonResponse(
        arbeidssoekerperioder = arbeidssoekerperioder,
        opplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker,
        profilering = profilering
    )
}
