package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.security.authentication.model.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {
    private val logger = buildLogger

    fun finnProfileringerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<ProfileringRow> =
        transaction(database) {
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
        transaction(database) {
            val rows = ProfileringFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.sendtInnAv.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.sendtInnAv.tidspunkt }.take(paging.size)
            }
        }

    fun hentProfileringForPeriodeIdOgOpplysningerId(periodeId: UUID, opplysningId: UUID): ProfileringRow? =
        transaction(database) {
            ProfileringFunctions.hentForPeriodeIdAndOpplysningId(periodeId, opplysningId)
        }

    fun lagreProfilering(profilering: Profilering) {
        transaction(database) {
            ProfileringFunctions.insert(profilering)
        }
    }

    fun lagreAlleProfileringer(profileringer: Sequence<Profilering>) {
        if (profileringer.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20
                profileringer.forEach { profilering ->
                    logger.debug("Lagrer ny profilering {}", profilering.id)
                    ProfileringFunctions.insert(profilering)
                }
            }
        }
    }
}
