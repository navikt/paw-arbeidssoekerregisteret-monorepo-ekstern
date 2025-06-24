package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap
import no.nav.paw.error.model.map
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.NavIdent
import no.nav.paw.oppslagapi.data.finnAlleIdenterForPerson
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.tilgangskontroll.client.RESPONSE_DATA_UNIT
import no.nav.paw.tilgangskontroll.client.Tilgang
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.client.feilVedIkkeTilgang
import java.time.Instant
import java.util.*

class AutorisasjonsTjeneste(
    private val tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte,
    private val kafkaKeysClient: KafkaKeysClient
) {

    /**
     * Autoriserer en bruker for tilgang til en eller flere identitetsnumre.
     * @param bruker Brukeren som ønsker tilgang.
     * @param oenskerTilgangTil Listen over identitetsnumre brukeren ønsker tilgang til.
     * @return Response<Unit> som indikerer om autoriseringen var vellykket eller ikke.
     */
    suspend fun <A> autoriser(
        bruker: Bruker<out Any>,
        oenskerTilgangTil: List<Identitetsnummer>,
        function: () -> A
    ): Response<A> {
        return when (bruker) {
            is Anonym -> ikkeTilgangProblemDetails()
            is NavAnsatt -> autoriserAnsatt(
                navIdent = NavIdent(bruker.ident),
                identitetsnummer = oenskerTilgangTil
            )
            is Sluttbruker -> autoriserSluttbruker(bruker, oenskerTilgangTil)
        }.map { function() }
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