package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.security.authentication.model.Bruker

class ApplicationQueryLogic(
    private val autorisasjonsTjeneste: AutorisasjonsTjeneste,
    private val databaseQuerySupport: DatabaseQeurySupport
) {

    suspend fun hentBekreftelser(
        bruker: Bruker<out Any>,
        request: ApiV2BekreftelserPostRequest
    ): Response<BekreftelserResponse> {
        if (request.perioder.isEmpty()) {
            return Data(BekreftelserResponse(emptyList()))
        }
        val tidslinjer = request.perioder.mapNotNull { periodeId ->
            val rader = databaseQuerySupport.hentRaderForPeriode(periodeId)
            genererTidslinje(periodeId, rader)
        }
        val identerOensketInfoOm = tidslinjer.map { it.identitetsnummer }.toSet()
        return autorisasjonsTjeneste.autoriser(
            handling = "Hent bekreftelser for arbeidssøkerperiode",
            bruker = bruker,
            oenskerTilgangTil = identerOensketInfoOm.map(::Identitetsnummer),
        ) {
            BekreftelserResponse(
                bekreftelser = tidslinjer.flatMap { it.bekreftelser }
            )
        }
    }
}