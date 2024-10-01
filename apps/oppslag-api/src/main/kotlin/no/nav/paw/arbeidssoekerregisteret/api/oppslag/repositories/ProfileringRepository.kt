package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnProfileringer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.opprettProfilering
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {

    fun hentProfilering(periodeId: UUID): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringer(periodeId)
        }

    fun hentProfilering(identitetsnummer: Identitetsnummer): List<ProfileringResponse> =
        transaction(database) {
            finnProfileringer(identitetsnummer)
        }

    fun lagreProfilering(profilering: Profilering) {
        transaction(database) {
            opprettProfilering(profilering)
        }
    }

    fun lagreProfileringer(profileringer: Sequence<Profilering>) {
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
