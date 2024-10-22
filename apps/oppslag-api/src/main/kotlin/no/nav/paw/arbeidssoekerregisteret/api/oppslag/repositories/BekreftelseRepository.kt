package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BekreftelseFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BekreftelseRepository(private val database: Database) {
    private val logger = buildLogger

    fun finnBekreftelserForPeriodeId(periodeId: UUID): List<BekreftelseRow> =
        transaction(database) {
            BekreftelseFunctions.findForPeriodeId(periodeId)
        }

    fun lagreAlleBekreftelser(bekreftelser: Sequence<Bekreftelse>) =
        transaction(database) {
            bekreftelser.forEach { bekreftelse ->
                if (BekreftelseFunctions.getForBekreftelseId(bekreftelse.id) != null) {
                    logger.warn("Ignorerer mottatt bekreftelse {} som duplikat", bekreftelse.id)
                } else {
                    logger.debug("Lagrer ny bekreftelse {}", bekreftelse.id)
                    BekreftelseFunctions.insert(bekreftelse)
                }
            }
        }
}