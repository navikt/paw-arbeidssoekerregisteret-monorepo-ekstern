package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap
import no.nav.paw.error.model.map
import no.nav.paw.error.model.onFailure
import no.nav.paw.error.model.onSuccess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.oppslagapi.data.finnAlleIdenterForPerson
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.tilgangskontroll.client.RESPONSE_DATA_UNIT
import no.nav.paw.tilgangskontroll.client.Tilgang
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.client.feilVedIkkeTilgang
import java.time.Instant
import java.util.*

class AutorisasjonsTjeneste(
    private val tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte,
    private val kafkaKeysClient: KafkaKeysClient,
    private val auditLogger: AuditLogger
) {

    /**
     * Autoriserer en bruker for tilgang til en eller flere identitetsnumre.
     * @param bruker Brukeren som ønsker tilgang.
     * @param oenskerTilgangTil Listen over identitetsnumre brukeren ønsker tilgang til.
     * @return Response<Unit> som indikerer om autoriseringen var vellykket eller ikke.
     */
    suspend fun <A> autoriser(
        handling: String,
        securityContext: SecurityContext,
        oenskerTilgangTil: List<Identitetsnummer>,
        function: () -> A
    ): Response<A> {
        val bruker = securityContext.bruker
        return when (bruker) {
            is Anonym -> autoriserAnonym(
                securityContext = securityContext
            )

            is NavAnsatt -> autoriserAnsatt(
                navIdent = NavIdent(bruker.ident),
                identitetsnummer = oenskerTilgangTil
            )

            is Sluttbruker -> autoriserSluttbruker(
                bruker = bruker,
                identitetsnummer = oenskerTilgangTil
            )
        }.onSuccess{
            Span.current().addEvent(
            "autorisering_ok", Attributes.of(
                stringKey("brukertype"), bruker::class.simpleName?.lowercase() ?: "null",
            ))
        }.onFailure {
            Span.current().addEvent(
            "autorisering_avvist", Attributes.of(
                stringKey("brukertype"), bruker::class.simpleName?.lowercase() ?: "null",
            ))
        }.map {
            val brukerIdent = when (bruker) {
                is NavAnsatt -> bruker.ident
                is Sluttbruker -> bruker.ident.verdi
                else -> null
            }
            if (brukerIdent != null) {
                oenskerTilgangTil.forEach { sluttbruker ->
                    auditLogger.audit(
                        melding = handling,
                        aktorIdent = brukerIdent,
                        sluttbrukerIdent = sluttbruker.verdi,
                        event = CefMessageEvent.ACCESS,
                        runtimeEnvironment = currentRuntimeEnvironment
                    )
                }
            }
            function()
        }
    }

    @WithSpan
    suspend fun autoriserSluttbruker(
        bruker: Sluttbruker,
        identitetsnummer: List<Identitetsnummer>,
    ): Response<Unit> {
        return kafkaKeysClient.finnAlleIdenterForPerson(bruker.ident)
            .flatMap { allIdenterForBruker ->
                if (identitetsnummer.any { it !in allIdenterForBruker }) {
                    ikkeTilgangProblemDetails()
                } else {
                    RESPONSE_DATA_UNIT
                }
            }
    }

    @WithSpan
    suspend fun autoriserAnsatt(
        navIdent: NavIdent,
        identitetsnummer: List<Identitetsnummer>
    ): Response<Unit> {
        return identitetsnummer.map { identitetsnummer ->
            tilgangsTjenesteForAnsatte.harAnsattTilgangTilPerson(navIdent, identitetsnummer, Tilgang.LESE)
        }.map(Response<Boolean>::feilVedIkkeTilgang)
            .filterIsInstance<ProblemDetails>()
            .firstOrNull() ?: return RESPONSE_DATA_UNIT
    }

    @WithSpan
    fun autoriserAnonym(securityContext: SecurityContext): Response<Unit> {
        val accessToken = securityContext.accessToken
        return if (accessToken.isM2MToken()) {
            RESPONSE_DATA_UNIT
        } else {
            ikkeTilgangProblemDetails()
        }
    }

    private fun ikkeTilgangProblemDetails(): ProblemDetails = ProblemDetails(
        id = UUID.randomUUID(),
        type = ErrorType
            .domain("tilgangskontroll")
            .error("ikke_tilgang")
            .build(),
        status = HttpStatusCode.Forbidden,
        title = "Ikke tilgang",
        instance = "instance??",
        timestamp = Instant.now()
    )
}