package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.EgenvurderingDbFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.EgenvurderingRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TraceParent
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.initSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class EgenvurderingRepository {
    private val logger = buildLogger

    fun finnEgenvurderingerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<EgenvurderingRow> =
        transaction {
            val rows = EgenvurderingDbFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun finnEgenvurderingerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<EgenvurderingRow> =
        transaction {
            val rows = EgenvurderingDbFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun hentEgenvurderingForPeriodeIdOgProfileringId(periodeId: UUID, profileringId: UUID): List<EgenvurderingRow> =
        transaction {
            EgenvurderingDbFunctions.getForPeriodeIdAndProfileringId(periodeId, profileringId)
        }

    fun lagreEgenvurdering(egenvurdering: Egenvurdering) {
        transaction {
            val eksisterendeEgenvurdering = EgenvurderingDbFunctions.getForEgenvurderingId(egenvurdering.id)
            if (eksisterendeEgenvurdering != null) {
                logger.warn("Ignorerer mottatt egenvurdering som duplikat")
            } else {
                logger.info("Lagrer ny egenvurdering")
                EgenvurderingDbFunctions.insert(egenvurdering)
            }
        }
    }

    fun lagreEgenvurderinger(egenvurderinger: Iterable<Pair<TraceParent?, Egenvurdering>>) {
        transaction {
            egenvurderinger.forEach { (traceparent, egenvurdering) ->
                initSpan(traceparent, "paw.kafka.consumer.egenvurdering", "egenvurdering process")
                    .use {
                        logger.info("Lagrer egenvurdering")
                        val eksisterendeEgenvurdering = EgenvurderingDbFunctions.getForEgenvurderingId(egenvurdering.id)
                        if (eksisterendeEgenvurdering != null) {
                            logger.warn("Ignorerer mottatt egenvurdering som duplikat")
                        } else {
                            logger.info("Lagrer ny egenvurdering")
                            EgenvurderingDbFunctions.insert(egenvurdering)
                        }
                    }
            }
        }
    }
}
