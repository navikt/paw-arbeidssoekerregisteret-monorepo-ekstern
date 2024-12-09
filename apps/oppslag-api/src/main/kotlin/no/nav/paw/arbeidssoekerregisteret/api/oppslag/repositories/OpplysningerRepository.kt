package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.security.authentication.model.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerRepository(private val database: Database) {

    private val logger = buildLogger

    fun finnOpplysningerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<OpplysningerRow> =
        transaction(database) {
            val rows = OpplysningerFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun finnOpplysningerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<OpplysningerRow> =
        transaction(database) {
            val periodeIdList = PeriodeFunctions.findPeriodeIdForIdentitetsnummerList(identitetsnummerList)
            val rows = OpplysningerFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker) {
        transaction(database) {
            val eksisterendeOpplysninger = OpplysningerFunctions.getRow(opplysninger.id)

            if (eksisterendeOpplysninger != null) {
                logger.warn("Opplysning med samme ID finnes allerede i databasen, ignorer derfor ny opplysning som duplikat")
            } else {
                OpplysningerFunctions.insert(opplysninger)
            }
        }
    }

    fun lagreAlleOpplysninger(opplysninger: Sequence<OpplysningerOmArbeidssoeker>) {
        if (opplysninger.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20

                val opplysningerIdList = opplysninger.map { it.id }.toList()
                val eksisterendeOpplysningerList = OpplysningerFunctions.finnRows(opplysningerIdList)
                val eksisterendeOpplysningerMap = eksisterendeOpplysningerList.associateBy { it.opplysningerId }

                opplysninger.forEach { opplysninger ->
                    val eksisterendeOpplysninger = eksisterendeOpplysningerMap[opplysninger.id]
                    if (eksisterendeOpplysninger != null) {
                        logger.warn("Ignorerer mottatte opplysninger {} som duplikat", opplysninger.id)
                    } else {
                        logger.debug("Lagrer nye opplysninger {}", opplysninger.id)
                        OpplysningerFunctions.insert(opplysninger)
                    }
                }
            }
        }
    }
}
