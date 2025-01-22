package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfilering
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class ProfileringRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var profileringRepository: ProfileringRepository

    beforeSpec {
        dataSource = initTestDatabase()
        Database.connect(dataSource)
        profileringRepository = ProfileringRepository()
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent ut en profilering" {
        val profilering =
            TestData.nyProfileringRow(periodeId = TestData.periodeId1, opplysningerId = TestData.opplysningerId1)
        profileringRepository.lagreProfilering(profilering.toProfilering())

        val profileringResponser =
            profileringRepository.finnProfileringerForPeriodeIdList(listOf(profilering.periodeId))

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Opprett og hent ut flere profileringer" {
        val profilering1 = TestData.nyProfileringRow(
            periodeId = TestData.periodeId2,
            opplysningerId = TestData.opplysningerId2,
            sendtInAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(1)))
        )
        val profilering2 = TestData.nyProfileringRow(
            periodeId = TestData.periodeId2,
            opplysningerId = TestData.opplysningerId3,
            sendtInAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(2)))
        )
        profileringRepository.lagreProfilering(profilering1.toProfilering())
        profileringRepository.lagreProfilering(profilering2.toProfilering())

        val profileringResponser = profileringRepository.finnProfileringerForPeriodeIdList(listOf(TestData.periodeId2))

        profileringResponser.size shouldBe 2
        val profileringResponse1 = profileringResponser[0]
        val profileringResponse2 = profileringResponser[1]
        profileringResponse1 shouldBeEqualTo profilering1
        profileringResponse2 shouldBeEqualTo profilering2
    }

    "Hent ut profilering med PeriodeId" {
        val profilering = TestData.nyProfileringRow(
            periodeId = TestData.periodeId3,
            opplysningerId = TestData.opplysningerId4
        )
        profileringRepository.lagreProfilering(profilering.toProfilering())
        val profileringResponser = profileringRepository.finnProfileringerForPeriodeIdList(listOf(TestData.periodeId3))

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Hent ut ikke-eksisterende profilering" {
        val profileringResponser = profileringRepository.finnProfileringerForPeriodeIdList(listOf(UUID.randomUUID()))

        profileringResponser.size shouldBe 0
    }

    "Lagre profileringer med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val profilering1 = TestData.nyProfileringRow(
            periodeId = periodeId,
            opplysningerId = UUID.randomUUID(),
            sendtInAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(1)))
        )
        val profilering2 = TestData.nyProfileringRow(
            periodeId = periodeId,
            opplysningerId = UUID.randomUUID(),
            sendtInAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(2)))
        )
        val profilering3 = TestData.nyProfileringRow(
            periodeId = periodeId,
            opplysningerId = UUID.randomUUID(),
            sendtInAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(3)))
        )
        val profileringer = listOf(
            profilering1.toProfilering(),
            profilering2.toProfilering(),
            profilering3.toProfilering()
        )
        profileringRepository.lagreProfileringer(profileringer)

        val lagredeProfileringer = profileringRepository.finnProfileringerForPeriodeIdList(listOf(periodeId))

        lagredeProfileringer.size shouldBeExactly 3
        val lagredeProfilering1 = lagredeProfileringer[0]
        val lagredeProfilering2 = lagredeProfileringer[1]
        val lagredeProfilering3 = lagredeProfileringer[2]
        lagredeProfilering1 shouldBeEqualTo profilering1
        lagredeProfilering2 shouldBeEqualTo profilering2
        lagredeProfilering3 shouldBeEqualTo profilering3
    }
})
