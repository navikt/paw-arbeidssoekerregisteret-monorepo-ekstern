package no.nav.paw.oppslagapi.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.SamletInformasjonResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.oppslagapi.respondWith
import no.nav.paw.oppslagapi.v2TilV1.v1Bekreftelser
import no.nav.paw.oppslagapi.v2TilV1.v1Opplysninger
import no.nav.paw.oppslagapi.v2TilV1.v1Periode
import no.nav.paw.oppslagapi.v2TilV1.v1Profileringer
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering

const val V1_API_SAMLET_INFORMASJON = "samlet-informasjon"
const val V1_API_VEILEDER_SAMLET_INFORMASJON = "veileder/samlet-informasjon"

fun Route.v1SamletInformasjon(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = TokenX) {
        get(V1_API_SAMLET_INFORMASJON) {
            val securityContext = call.securityContext()
            val bruker = (securityContext.bruker as? Sluttbruker)
                ?: throw IllegalArgumentException("Ugyldig token type, forventet Sluttbruker")
            val bareReturnerSiste = call.bareReturnerSiste()
            val response: Response<SamletInformasjonResponse> = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = bruker.ident
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tilSamletInformasjon(it, bareReturnerSiste) }
            call.respondWith(response)
        }
    }

}

fun Route.v1VeilederSamletInformasjon(
    appQueryLogic: ApplicationQueryLogic
) {
    autentisering(issuer = AzureAd) {
        post<ArbeidssoekerperiodeRequest>(V1_API_VEILEDER_SAMLET_INFORMASJON) { request ->
            val securityContext = call.securityContext()
            val identitetsnummer = Identitetsnummer(request.identitetsnummer)
            val bareReturnerSiste = call.bareReturnerSiste()
            val response = appQueryLogic.hentTidslinjer(
                securityContext = securityContext,
                identitetsnummer = identitetsnummer
            ).map {
                if (bareReturnerSiste) listOfNotNull(it.gjeldeneEllerSisteTidslinje())
                else it
            }.map { tilSamletInformasjon(it, bareReturnerSiste) }
            call.respondWith(response)
        }
    }

}

private fun tilSamletInformasjon(
    tidslinjer: List<Tidslinje>,
    bareSiste: Boolean
): SamletInformasjonResponse = tidslinjer.fold(
    SamletInformasjonResponse(
        arbeidssoekerperioder = emptyList(),
        opplysningerOmArbeidssoeker = emptyList(),
        profilering = emptyList(),
        bekreftelser = emptyList()
    )
) { samletInfo, tidslinje ->
    val antall = if (bareSiste) 1 else Int.MAX_VALUE
    val periode = tidslinje.v1Periode()
    val opplysningerOmArbeidssoeker = tidslinje
        .v1Opplysninger()
        .sortedByDescending { it.sendtInnAv.tidspunkt }
        .takeLast(antall)
    val bekreftelser = tidslinje
        .v1Bekreftelser()
        .sortedByDescending { it.svar.gjelderTil }
        .takeLast(antall)
    val profileringer = tidslinje
        .v1Profileringer()
        .sortedByDescending { it.sendtInnAv.tidspunkt}
        .takeLast(antall)

    SamletInformasjonResponse(
        arbeidssoekerperioder = samletInfo.arbeidssoekerperioder + periode,
        opplysningerOmArbeidssoeker = samletInfo.opplysningerOmArbeidssoeker + opplysningerOmArbeidssoeker,
        profilering = samletInfo.profilering + profileringer,
        bekreftelser = samletInfo.bekreftelser + bekreftelser
    )
}



