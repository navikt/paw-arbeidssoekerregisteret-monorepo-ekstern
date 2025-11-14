package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.v2TilV1.v1Profileringer
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import java.util.UUID

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
            val response: Response<List<ProfileringResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer
                    .flatMap { tidslinje -> tidslinje.v1Profileringer() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.sendtInnAv.tidspunkt })
                        } else {
                            response
                        }
                    }

            }
            call.respondWith(response)
        }
        get("$V1_API_PROFILERING/{periodeId}") {
            val siste = call.bareReturnerSiste()
            val periodeId = call.parameters["periodeId"]?.let(UUID::fromString)
                ?: throw IllegalArgumentException("PeriodeId må spesifiseres i URL")
            val securityContext = call.securityContext()
            val response: Response<List<ProfileringResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                perioder = listOf(periodeId)
            ).map { tidslinje ->
                tidslinje.firstOrNull()?.v1Profileringer() ?: emptyList()
            }.map { profileringer ->
                if (siste) listOf(profileringer.maxByOrNull { it.sendtInnAv.tidspunkt }) else profileringer
            }.map { it.filterNotNull() }
            call.respondWith(response)
        }
    }
}

fun Route.v1VeilederProfilering(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<ProfileringRequest>(V1_API_VEILEDER_PROFILERING) { request ->
            val securityContext = call.securityContext()
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<ProfileringResponse>> = (request.periodeId?.let { periodeId ->
                appQueryLogic.hentTidslinjer(
                    securityContext = securityContext,
                    perioder = listOf(periodeId)
                )
            } ?: appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = Identitetsnummer(request.identitetsnummer)
            )).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer.flatMap { tidslinje -> tidslinje.v1Profileringer() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.sendtInnAv.tidspunkt })
                        } else {
                            response
                        }
                    }
            }
            call.respondWith(response)
        }
    }
}
