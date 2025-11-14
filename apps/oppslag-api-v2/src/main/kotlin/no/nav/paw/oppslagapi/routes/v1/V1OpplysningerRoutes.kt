package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.v2TilV1.v1Opplysninger
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import java.util.UUID

const val V1_API_OPPLYSNINGER = "opplysninger-om-arbeidssoeker"
const val V1_API_VEILEDER_OPPLYSNINGER = "veileder/opplysninger-om-arbeidssoeker"

fun Route.v1Opplysninger(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get(V1_API_OPPLYSNINGER) {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<OpplysningerOmArbeidssoekerResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer
                    .flatMap { tidslinje ->tidslinje.v1Opplysninger() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.sendtInnAv.tidspunkt } )
                        } else {
                            response
                        }
                    }

            }
            call.respondWith(response)
        }

        get("$V1_API_OPPLYSNINGER/{periodeId}") {
            val siste = call.bareReturnerSiste()
            val periodeId = call.parameters["periodeId"]?.let(UUID::fromString)
                ?: throw IllegalArgumentException("PeriodeId må spesifiseres i URL")
            val securityContext = call.securityContext()
            val response: Response<List<OpplysningerOmArbeidssoekerResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                perioder = listOf(periodeId)
            ).map { tidslinje ->
                tidslinje.firstOrNull()?.v1Opplysninger() ?: emptyList()
            }.map { profileringer ->
                if (siste) listOfNotNull(profileringer.maxByOrNull { it.sendtInnAv.tidspunkt }) else profileringer
            }
            call.respondWith(response)
        }
    }
}

fun Route.v1VeilederOpplysninger(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<OpplysningerOmArbeidssoekerRequest>(V1_API_VEILEDER_OPPLYSNINGER) { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<OpplysningerOmArbeidssoekerResponse>> = (request.periodeId?.let { periodeId ->
                appQueryLogic.hentTidslinjer(
                    securityContext = securityContext,
                    perioder = listOf(periodeId)
                )
            } ?: appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = identitetsnummer
            )).map {
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
            call.respondWith(response)
        }
    }
}
