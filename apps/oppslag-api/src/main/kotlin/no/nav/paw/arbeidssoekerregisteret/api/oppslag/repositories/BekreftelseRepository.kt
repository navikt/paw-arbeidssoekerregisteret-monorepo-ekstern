package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.finnBekreftelserForPeriodeId
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BekreftelseRepository(private val database: Database) {

    fun finnBekreftelserForPeriodeId(periodeId: UUID): List<BekreftelseResponse> =
        transaction(database) {
            finnBekreftelserForPeriodeId(periodeId)
        }

    fun hentBekreftelseForId(bekreftelseMeldingId: UUID): BekreftelseResponse? =
        transaction(database) {
            hentBekreftelseForId(bekreftelseMeldingId)
        }

    fun opprettBekreftelse(bekreftelse: Bekreftelse) {
        transaction(database) {
            opprettBekreftelse(bekreftelse)
        }
    }
}