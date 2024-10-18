package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.finnProfileringer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.opprettProfilering
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {

    fun finnProfileringerForPeriodeId(periodeId: UUID): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringer(periodeId)
        }

    fun finnProfileringerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringer(identitetsnummerList)
        }

    fun lagreProfilering(profilering: Profilering) {
        transaction(database) {
            opprettProfilering(profilering)
        }
    }

    fun lagreAlleProfileringer(profileringer: Sequence<Profilering>) {
        if (profileringer.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20
                profileringer.forEach { profilering ->
                    opprettProfilering(profilering)
                }
            }
        }
    }
}
