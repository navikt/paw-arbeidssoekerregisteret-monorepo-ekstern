package no.nav.paw.oppslagapi.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.map
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.v2TilV1.v1Opplysninger
import no.nav.paw.oppslagapi.v2TilV1.v1Profileringer
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

const val V1_API_PROFILERING = "profilering"
const val V1_API_VEILEDER_PROFILERING = "veileder/profilering"

fun Route.v1Profilering(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get(V1_API_PROFILERING) {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val bareReturnerSiste = call.bareReturnerSiste()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer
                    .flatMap { tidslinje ->tidslinje.v1Profileringer() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.sendtInnAv.tidspunkt } )
                        } else {
                            response
                        }
                    }

            }
            when (response) {
                is Data<List<ProfileringResponse>> -> {
                    call.respond(status = HttpStatusCode.OK, message = response.data)
                }

                is ProblemDetails -> {
                    call.respond(status = response.status, message = response)
                }
            }
        }
    }
}

fun Route.v1VeilederProfilering(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<ArbeidssoekerperiodeRequest>(V1_API_VEILEDER_PROFILERING) { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            val bareReturnerSiste = call.bareReturnerSiste()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = identitetsnummer
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer.flatMap { tidslinje -> tidslinje.v1Opplysninger()}
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.sendtInnAv.tidspunkt } )
                        } else {
                            response
                        }
                    }
            }
            when (response) {
                is Data<List<OpplysningerOmArbeidssoekerResponse>> -> {
                    call.respond(status = HttpStatusCode.OK, message = response.data)
                }

                is ProblemDetails -> {
                    call.respond(status = response.status, message = response)
                }
            }
        }
    }
}
