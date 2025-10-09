package no.naw.paw.brukerprofiler.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.test.data.periode.createProfilering
import no.naw.paw.brukerprofiler.db.initDatabase
import no.naw.paw.brukerprofiler.domain.interntFormat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class LagreOgHentProfileringTest : FreeSpec({

    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    beforeSpec { Database.connect(dataSource) }

    "Lagring og henting av Profilering" - {
        val profilering = createProfilering()
        "Vi kan lagre profilering ${profilering.id} uten feil" {
            transaction {
                lagreProfilering(profilering)
            }
        }
        "Vi kan hente profilering ${profilering.id} for periode ${profilering.periodeId} fra databasen" {
            hentProfileringOrNull(profilering.periodeId) should { profileringFraDb ->
                profileringFraDb.shouldNotBeNull()
                profileringFraDb.profileringId shouldBe profilering.id
                profileringFraDb.periodeId shouldBe profilering.periodeId
                profileringFraDb.profileringTidspunkt shouldBe profilering.sendtInnAv.tidspunkt
                profileringFraDb.profileringResultat shouldBe profilering.profilertTil.interntFormat()
            }
        }
        "Vi får null når vi prøver å hente profilering med ukjent periodeId" {
            hentProfileringOrNull(UUID.randomUUID()) shouldBe null
        }
    }
})