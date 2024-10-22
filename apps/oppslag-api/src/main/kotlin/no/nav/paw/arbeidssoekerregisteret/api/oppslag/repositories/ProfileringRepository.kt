package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {
    private val logger = buildLogger

    fun finnProfileringerForPeriodeId(periodeId: UUID): List<ProfileringRow> =
        transaction(database) {
            ProfileringFunctions.findForPeriodeId(periodeId)
        }

    fun finnProfileringerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<ProfileringRow> =
        transaction(database) {
            ProfileringFunctions.findForIdentitetsnummerList(identitetsnummerList)
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
