package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TraceParent
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.initSpan
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.felles.model.Identitetsnummer
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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

    fun lagreOpplysninger(opplysning: OpplysningerOmArbeidssoeker) {
        transaction {
            val eksisterendeOpplysninger = OpplysningerFunctions.getForOpplysningerId(opplysning.id)
            if (eksisterendeOpplysninger != null) {
                logger.warn("Ignorerer mottatte opplysninger som duplikat")
            } else {
                logger.info("Lagrer nye opplysninger")
                OpplysningerFunctions.insert(opplysning)
            }
        }
    }

    fun lagreOpplysninger(opplysninger: Iterable<Pair<TraceParent?, OpplysningerOmArbeidssoeker>>) {
        transaction {
            opplysninger.forEach { (traceparent, opplysning) ->
                initSpan(traceparent, "paw.kafka.consumer.opplysninger", "opplysninger process")
                    .use {
                        logger.info("Lagrer opplysninger")
                        val eksisterendeOpplysninger = OpplysningerFunctions.getForOpplysningerId(opplysning.id)
                        if (eksisterendeOpplysninger != null) {
                            logger.warn("Ignorerer mottatte opplysninger som duplikat")
                        } else {
                            logger.info("Lagrer nye opplysninger")
                            OpplysningerFunctions.insert(opplysning)
                        }
                    }
            }
        }
    }
}
