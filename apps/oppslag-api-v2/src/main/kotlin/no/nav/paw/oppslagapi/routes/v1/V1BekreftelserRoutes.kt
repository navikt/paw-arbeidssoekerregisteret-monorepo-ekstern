package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerRequest
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.mapping.v1.v1Bekreftelser
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import java.util.*

fun Route.v1Bekrefelser(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get("/arbeidssoekerbekreftelser") {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<BekreftelseResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tidslinjer ->
                tidslinjer
                    .flatMap { tidslinje -> tidslinje.v1Bekreftelser() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.svar.sendtInnAv.tidspunkt })
                        } else {
                            response
                        }
                    }

            }
            call.respondWith(response)
        }

        get("/arbeidssoekerbekreftelser/{periodeId}") {
            val siste = call.bareReturnerSiste()
            val periodeId = call.parameters["periodeId"]?.let(UUID::fromString)
                ?: throw IllegalArgumentException("PeriodeId må spesifiseres i URL")
            val securityContext = call.securityContext()
            val response: Response<List<BekreftelseResponse>> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                perioder = listOf(periodeId)
            ).map { tidslinje ->
                tidslinje.firstOrNull()?.v1Bekreftelser() ?: emptyList()
            }.map { bekreftelser ->
                if (siste) listOfNotNull(bekreftelser.maxByOrNull { it.svar.sendtInnAv.tidspunkt }) else bekreftelser
            }
            call.respondWith(response)
        }
    }
}

fun Route.v1VeilederBekreftelser(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<OpplysningerOmArbeidssoekerRequest>("/veileder/arbeidssoekerbekreftelser") { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<List<BekreftelseResponse>> = (request.periodeId?.let { periodeId ->
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
                tidslinjer.flatMap { tidslinje -> tidslinje.v1Bekreftelser() }
                    .let { response ->
                        if (bareReturnerSiste) {
                            listOfNotNull(response.maxByOrNull { it.svar.sendtInnAv.tidspunkt })
                        } else {
                            response
                        }
                    }
            }
            call.respondWith(response)
        }
    }
}
