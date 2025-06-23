package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.appLogger
import no.nav.paw.security.authentication.model.Bruker
import org.jetbrains.exposed.sql.transactions.transaction

class ApplicationQueryLogic(
    private val autorisasjonsTjeneste: AutorisasjonsTjeneste
) {

    suspend fun hentBekreftelser(
        bruker: Bruker<out Any>,
        request: ApiV2BekreftelserPostRequest
    ): BekreftelserResponse {
        appLogger.debug("Henter bekreftelser...")
        if (request.perioder.isEmpty()) {
            return BekreftelserResponse(emptyList())
        }
        val tidslinjer = request.perioder.mapNotNull { periodeId ->
            val rader = transaction {
                hentForPeriode(periodeId)
            }
            genererTidslinje(periodeId, rader)
        }
        val identerOensketInfoOm = tidslinjer.map { it.identitetsnummer }.toSet()
        return autorisasjonsTjeneste.autoriser(
            bruker = bruker,
            oenskerTilgangTil = identerOensketInfoOm.map(::Identitetsnummer)
        ) {
            BekreftelserResponse(
                bekreftelser = tidslinjer.flatMap { it.bekreftelser }
            )
        }.getOrThrow()
    }
}