package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TraceParent
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.initSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository {
    private val logger = buildLogger

    fun finnProfileringerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<ProfileringRow> =
        transaction {
            val rows = ProfileringFunctions.findForPeriodeIdList(periodeIdList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun finnProfileringerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringRow> =
        transaction {
            val rows = ProfileringFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun hentProfileringForPeriodeIdOgOpplysningerId(periodeId: UUID, opplysningId: UUID): ProfileringRow? =
        transaction {
            ProfileringFunctions.getForPeriodeIdAndOpplysningId(periodeId, opplysningId)
        }

    fun lagreProfilering(profilering: Profilering) {
        transaction {
            val eksisterendeProfilering = ProfileringFunctions.getForProfileringId(profilering.id)
            if (eksisterendeProfilering != null) {
                logger.warn("Ignorerer mottatt profilering som duplikat")
            } else {
                logger.info("Lagrer ny profilering")
                ProfileringFunctions.insert(profilering)
            }
        }
    }

    fun lagreProfileringer(profileringer: Iterable<Pair<TraceParent?, Profilering>>) {
        transaction {
            profileringer.forEach { (traceparent, profilering) ->
                initSpan(traceparent, "paw.kafka.consumer.profilering", "profilering process")
                    .use {
                        logger.info("Lagrer profilering")
                        val eksisterendeProfilering = ProfileringFunctions.getForProfileringId(profilering.id)
                        if (eksisterendeProfilering != null) {
                            logger.warn("Ignorerer mottatt profilering som duplikat")
                        } else {
                            logger.info("Lagrer ny profilering")
                            ProfileringFunctions.insert(profilering)
                        }
                    }
            }
        }
    }
}
