package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.invalidTraceParent
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class EgenvurderingRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var egenvurderingRepository: EgenvurderingRepository

    beforeSpec {
        dataSource = initTestDatabase()
        Database.connect(dataSource)
        egenvurderingRepository = EgenvurderingRepository()
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent ut en egenvurdering" {
        val egenvurdering =
            TestData.nyEgenvurderingRow(periodeId = TestData.periodeId1, opplysningerOmArbeidssoekerId = TestData.opplysningerId1, profileringId = TestData.profileringId1)
        egenvurderingRepository.lagreEgenvurdering(egenvurdering.toEgenvurdering())

        val egenvurderingResponser =
            egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(listOf(egenvurdering.periodeId))

        egenvurderingResponser.size shouldBe 1
        val egenvurderingResponse = egenvurderingResponser[0]
        egenvurderingResponse shouldBeEqualTo egenvurdering
    }

    "Opprett og hent ut flere egenvurderinger" {
        val egenvurdering1 = TestData.nyEgenvurderingRow(
            periodeId = TestData.periodeId2,
            opplysningerOmArbeidssoekerId = TestData.opplysningerId2,
            profileringId = TestData.profileringId1,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(1)))
        )
        val egenvurdering2 = TestData.nyEgenvurderingRow(
            periodeId = TestData.periodeId2,
            opplysningerOmArbeidssoekerId = TestData.opplysningerId3,
            profileringId = TestData.profileringId2,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(2)))
        )
        egenvurderingRepository.lagreEgenvurdering(egenvurdering1.toEgenvurdering())
        egenvurderingRepository.lagreEgenvurdering(egenvurdering2.toEgenvurdering())

        val egenvurderingResponser = egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(listOf(TestData.periodeId2))

        egenvurderingResponser.size shouldBe 2
        val egenvurderingResponse1 = egenvurderingResponser[0]
        val egenvurderingResponse2 = egenvurderingResponser[1]
        egenvurderingResponse1 shouldBeEqualTo egenvurdering1
        egenvurderingResponse2 shouldBeEqualTo egenvurdering2
    }

    "Hent ut egenvurdering med PeriodeId" {
        val egenvurdering = TestData.nyEgenvurderingRow(
            periodeId = TestData.periodeId3,
            opplysningerOmArbeidssoekerId = TestData.opplysningerId4,
            profileringId = TestData.profileringId3,
        )
        egenvurderingRepository.lagreEgenvurdering(egenvurdering.toEgenvurdering())
        val egenvurderingResponser = egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(listOf(TestData.periodeId3))

        egenvurderingResponser.size shouldBe 1
        val egenvurderingResponse = egenvurderingResponser[0]
        egenvurderingResponse shouldBeEqualTo egenvurdering
    }

    "Hent ut ikke-eksisterende egenvurdering" {
        val egenvurderingResponser = egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(listOf(UUID.randomUUID()))

        egenvurderingResponser.size shouldBe 0
    }

    "Lagre egenvurderinger med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val egenvurdering1 = TestData.nyEgenvurderingRow(
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            profileringId = UUID.randomUUID(),
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(1)))
        )
        val egenvurdering2 = TestData.nyEgenvurderingRow(
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            profileringId = UUID.randomUUID(),
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(2)))
        )
        val egenvurdering3 = TestData.nyEgenvurderingRow(
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = UUID.randomUUID(),
            profileringId = UUID.randomUUID(),
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = Instant.now().minus(Duration.ofDays(3)))
        )
        val egenvurderinger = listOf(
            egenvurdering1.toEgenvurdering(),
            egenvurdering2.toEgenvurdering(),
            egenvurdering3.toEgenvurdering()
        )
        egenvurderingRepository.lagreEgenvurderinger(egenvurderinger.map { invalidTraceParent to it})

        val lagredeEgenvurderinger = egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(listOf(periodeId))

        lagredeEgenvurderinger.size shouldBeExactly 3
        val lagretEgenvurdering1 = lagredeEgenvurderinger[0]
        val lagretEgenvurdering2 = lagredeEgenvurderinger[1]
        val lagretEgenvurdering3 = lagredeEgenvurderinger[2]
        lagretEgenvurdering1 shouldBeEqualTo egenvurdering1
        lagretEgenvurdering2 shouldBeEqualTo egenvurdering2
        lagretEgenvurdering3 shouldBeEqualTo egenvurdering3
    }
})