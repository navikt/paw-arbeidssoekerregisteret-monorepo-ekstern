package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BekreftelseFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseRow
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BekreftelseRepository(private val database: Database) {

    fun finnBekreftelserForPeriodeId(periodeId: UUID): List<BekreftelseRow> =
        transaction(database) {
            BekreftelseFunctions.findForPeriodeId(periodeId)
        }

    fun opprettBekreftelse(bekreftelse: Bekreftelse) {
        transaction(database) {
            BekreftelseFunctions.insert(bekreftelse)
        }
    }
}