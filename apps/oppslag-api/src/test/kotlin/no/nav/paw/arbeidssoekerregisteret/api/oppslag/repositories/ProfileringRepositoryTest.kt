package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfilering
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.sql.DataSource

class ProfileringRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var repository: ProfileringRepository

    beforeSpec {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        repository = ProfileringRepository(database)
        val opplysningerRepository = OpplysningerRepository(database)
        val opplysninger1 = TestData
            .nyOpplysningerRow(opplysningerId = TestData.opplysningerId1, periodeId = TestData.periodeId1)
        val opplysninger2 = TestData
            .nyOpplysningerRow(opplysningerId = TestData.opplysningerId2, periodeId = TestData.periodeId2)
        opplysningerRepository.lagreOpplysninger(opplysninger1.toOpplysningerOmArbeidssoeker())
        opplysningerRepository.lagreOpplysninger(opplysninger2.toOpplysningerOmArbeidssoeker())
    }

    afterEach {
        slettAlleProfileringer(database)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent ut en profilering" {
        val profilering =
            TestData.nyProfileringRow(periodeId = TestData.periodeId1, opplysningerId = TestData.opplysningerId1)
        repository.lagreProfilering(profilering.toProfilering())

        val profileringResponser = repository.finnProfileringerForPeriodeIdList(listOf(profilering.periodeId))

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Opprett og hent ut flere profileringer" {
        val profilering1 =
            TestData.nyProfileringRow(periodeId = TestData.periodeId1, opplysningerId = TestData.opplysningerId1)
        val profilering2 =
            TestData.nyProfileringRow(periodeId = TestData.periodeId1, opplysningerId = TestData.opplysningerId2)
        repository.lagreProfilering(profilering1.toProfilering())
        repository.lagreProfilering(profilering2.toProfilering())

        val profileringResponser = repository.finnProfileringerForPeriodeIdList(listOf(TestData.periodeId1))

        profileringResponser.size shouldBe 2
        val profileringResponse1 = profileringResponser[0]
        val profileringResponse2 = profileringResponser[1]
        profileringResponse1 shouldBeEqualTo profilering1
        profileringResponse2 shouldBeEqualTo profilering2
    }

    "Hent ut profilering med PeriodeId" {
        val profilering =
            TestData.nyProfileringRow(periodeId = TestData.periodeId1, opplysningerId = TestData.opplysningerId1)
        repository.lagreProfilering(profilering.toProfilering())
        val profileringResponser = repository.finnProfileringerForPeriodeIdList(listOf(TestData.periodeId1))

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Hent ut ikke-eksisterende profilering" {
        val profileringResponser = repository.finnProfileringerForPeriodeIdList(listOf(UUID.randomUUID()))

        profileringResponser.size shouldBe 0
    }

    "Lagre profileringer med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val profilering1 = TestData.nyProfileringRow(periodeId = periodeId, opplysningerId = UUID.randomUUID())
        val profilering2 = TestData.nyProfileringRow(periodeId = periodeId, opplysningerId = UUID.randomUUID())
        val profilering3 = TestData.nyProfileringRow(periodeId = periodeId, opplysningerId = UUID.randomUUID())
        val profileringer =
            sequenceOf(profilering1.toProfilering(), profilering2.toProfilering(), profilering3.toProfilering())
        repository.lagreAlleProfileringer(profileringer)

        val lagredeProfileringer = repository.finnProfileringerForPeriodeIdList(listOf(periodeId))

        lagredeProfileringer.size shouldBeExactly 3
        val lagredeProfilering1 = lagredeProfileringer[0]
        val lagredeProfilering2 = lagredeProfileringer[1]
        val lagredeProfilering3 = lagredeProfileringer[2]
        lagredeProfilering1 shouldBeEqualTo profilering1
        lagredeProfilering2 shouldBeEqualTo profilering2
        lagredeProfilering3 shouldBeEqualTo profilering3
    }
})

private fun slettAlleProfileringer(database: Database) {
    transaction(database) {
        ProfileringTable.deleteAll()
    }
}
