package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerRepository {

    private val logger = buildLogger

    fun finnOpplysningerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<OpplysningerRow> =
        transaction {
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
        transaction {
            val periodeIdList = PeriodeFunctions.findPeriodeIdForIdentitetsnummerList(identitetsnummerList)
            val rows = OpplysningerFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker) {
        transaction {
            val eksisterendeOpplysninger = OpplysningerFunctions.getForOpplysningerId(opplysninger.id)
            if (eksisterendeOpplysninger != null) {
                logger.warn("Ignorerer mottatte opplysninger som duplikat")
            } else {
                logger.info("Lagrer nye opplysninger")
                OpplysningerFunctions.insert(opplysninger)
            }
        }
    }
}
