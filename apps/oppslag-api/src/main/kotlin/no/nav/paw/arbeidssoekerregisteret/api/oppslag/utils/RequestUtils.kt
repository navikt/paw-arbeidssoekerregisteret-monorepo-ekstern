package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toIdentitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.StatusException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.*

suspend inline fun <reified T : Any> ApplicationCall.getRequestBody(): T =
    try {
        receive<T>()
    } catch (e: Exception) {
        throw StatusException(HttpStatusCode.BadRequest, "Kunne ikke deserialisere request-body")
    }

fun ApplicationCall.getClaim(
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

private fun ApplicationCall.getNavAnsattAzureId(): String = getClaim("azure", "oid") ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'oid'-claim i token fra azure-issuer")

private fun ApplicationCall.getNavAnsattIdent(): String = getClaim("azure", "NAVident") ?: throw StatusException(HttpStatusCode.Forbidden, "Fant ikke 'NAVident'-claim i token fra azure-issuer")

fun ApplicationCall.getNavAnsatt(): NavAnsatt =
    NavAnsatt(
        getNavAnsattAzureId(),
        getNavAnsattIdent()
    )

fun ApplicationCall.getNavAnsattWithNavIdentFromHeader(navIdent: String): NavAnsatt =
    NavAnsatt(
        getNavAnsattAzureId(),
        navIdent
    )

fun ApplicationCall.isMachineToMachineToken(): Boolean =
    authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("azure")
        ?.getAsList("roles")
        ?.contains("access_as_application")
        ?: false

fun ApplicationCall.getNavAnsattFromToken(): NavAnsatt? =
    if (this.isMachineToMachineToken()) {
        this.request.headers["Nav-Ident"]
            ?.let { navIdent -> getNavAnsattWithNavIdentFromHeader(navIdent) }
    } else {
        this.getNavAnsatt()
    }

suspend fun ApplicationCall.verifyAccessFromToken(
    autorisasjonService: AutorisasjonService,
    identitetsnummer: Identitetsnummer
): Boolean {
    val navAnsatt =
        getNavAnsattFromToken()
            ?: return true

    buildLogger.info("Sjekker om NAV-ansatt har tilgang til bruker")
    return autorisasjonService.verifiserTilgangTilBruker(navAnsatt, identitetsnummer).also { harTilgang ->
        if (!harTilgang) {
            buildLogger.warn("NAV-ansatt har ikke tilgang til bruker")
            respondText(status = HttpStatusCode.Forbidden, text = HttpStatusCode.Forbidden.description)
        }
    }
}

suspend fun ApplicationCall.verifyPeriodeId(
    periodeId: UUID,
    identitetsnummer: Identitetsnummer,
    arbeidssoekerperiodeService: ArbeidssoekerperiodeService
): Boolean {
    val periodeIdTilhoererIdentitetsnummer = arbeidssoekerperiodeService.periodeIdTilhoererIdentitetsnummer(periodeId, identitetsnummer)
    if (!periodeIdTilhoererIdentitetsnummer) {
        buildLogger.warn("PeriodeId tilhører ikke bruker: $periodeId")
        respondText(status = HttpStatusCode.Forbidden, text = "PeriodeId tilhører ikke bruker: $periodeId")
        return false
    }
    return true
}

suspend fun PipelineContext<Unit, ApplicationCall>.isPeriodeIdValid(
    periodeId: UUID?,
    identitetsnummer: Identitetsnummer,
    arbeidssoekerperiodeService: ArbeidssoekerperiodeService
): Boolean {
    if (periodeId != null) {
        val periodeIdTilhoererIdentitetsnummer = call.verifyPeriodeId(periodeId, identitetsnummer, arbeidssoekerperiodeService)
        if (!periodeIdTilhoererIdentitetsnummer) {
            buildLogger.warn("PeriodeId tilhører ikke bruker: $periodeId")
            call.respond(HttpStatusCode.Forbidden, "PeriodeId tilhører ikke bruker: $periodeId")
            return false
        }
    }
    return true
}

fun createSisteSamletInformasjonResponse(
    arbeidssoekerperioder: List<ArbeidssoekerperiodeResponse>,
    opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    profileringService: ProfileringService
): SamletInformasjonResponse {
    val sistePeriode = arbeidssoekerperioder.maxByOrNull { it.startet.tidspunkt }
    val sisteOpplysninger =
        sistePeriode?.let { periode ->
            opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoeker(periode.periodeId).maxByOrNull { it.sendtInnAv.tidspunkt }
        }
    val sisteProfilering =
        sistePeriode?.let { periode ->
            profileringService.hentProfileringForArbeidssoekerMedPeriodeId(periode.periodeId).maxByOrNull { it.sendtInnAv.tidspunkt }
        }

    return SamletInformasjonResponse(
        arbeidssoekerperioder = listOfNotNull(sistePeriode),
        opplysningerOmArbeidssoeker = listOfNotNull(sisteOpplysninger),
        profilering = listOfNotNull(sisteProfilering)
    )
}

fun createSamletInformasjonResponse(
    arbeidssoekerperioder: List<ArbeidssoekerperiodeResponse>,
    identitetsnummer: String,
    opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    profileringService: ProfileringService
): SamletInformasjonResponse {
    val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerService.hentOpplysningerOmArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))
    val profilering = profileringService.hentProfileringForArbeidssoekerMedIdentitetsnummer(Identitetsnummer(identitetsnummer))

    return SamletInformasjonResponse(
        arbeidssoekerperioder = arbeidssoekerperioder,
        opplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker,
        profilering = profilering
    )
}
