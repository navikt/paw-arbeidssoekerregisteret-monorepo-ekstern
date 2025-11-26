package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.EgenvurderingIkkeStoettetException
import no.nav.paw.arbeidssoekerregisteret.exception.ProfileringIkkeStoettetException
import no.nav.paw.arbeidssoekerregisteret.mapping.asDto
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurdertTil
import no.nav.paw.arbeidssoekerregisteret.model.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.repository.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.felles.model.ArbeidssoekerId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.RecordKey
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Identitet
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil as AvroProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering as AvroEgenvurdering

class EgenvurderingServiceTest : FreeSpec({

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val dataSource = autoClose(initTestDatabase())
    val kafkaKeysClientMock = mockk<KafkaKeysClient>(relaxed = true)
    val egenvurderingKafkaProducerMock = mockProducer()
    val egenvurderingRepository = EgenvurderingPostgresRepository

    val egenvurderingService = EgenvurderingService(
        applicationConfig = applicationConfig,
        kafkaKeysClient = kafkaKeysClientMock,
        egenvurderingKafkaProducer = egenvurderingKafkaProducerMock,
        egenvurderingRepository = egenvurderingRepository,
    )
    val prodsettingstidspunkt = Instant.now().minusSeconds(60)
    val justerApplicationConfig = applicationConfig.copy(
        prodsettingstidspunktEgenvurdering = prodsettingstidspunkt
    )
    val justertEgenvurderingService = EgenvurderingService(
        applicationConfig = justerApplicationConfig,
        kafkaKeysClient = kafkaKeysClientMock,
        egenvurderingKafkaProducer = egenvurderingKafkaProducerMock,
        egenvurderingRepository = egenvurderingRepository,
    )

    val fnr1 = Identitetsnummer("01017012345")
    val fnr2 = Identitetsnummer("02017012345")
    val dnr2 = Identitetsnummer("42017012345")
    val fnr3 = Identitetsnummer("03017012345")
    val fnr4 = Identitetsnummer("04017012345")
    val fnr5 = Identitetsnummer("05017012345")
    val fnr6 = Identitetsnummer("06017012345")
    val fnr7 = Identitetsnummer("07017012345")
    val fnr8 = Identitetsnummer("08017012345")
    val fnr9 = Identitetsnummer("09017012345")
    val fnr10 = Identitetsnummer("10017012345")
    val arbId4 = ArbeidssoekerId(1004)
    val key4 = RecordKey(-1004)
    val periodeId1 = UUID.randomUUID()
    val periodeId2 = UUID.randomUUID()
    val periodeId4 = UUID.randomUUID()
    val periodeId5 = UUID.randomUUID()
    val periodeId6 = UUID.randomUUID()
    val periodeId8 = UUID.randomUUID()
    val periodeId9 = UUID.randomUUID()
    val periodeId10 = UUID.randomUUID()
    val profileringId1 = UUID.randomUUID()
    val profileringId2 = UUID.randomUUID()
    val profileringId4 = UUID.randomUUID()
    val profileringId5 = UUID.randomUUID()
    val profileringId6 = UUID.randomUUID()
    val profileringId7 = UUID.randomUUID()
    val profileringId8 = UUID.randomUUID()
    val profileringId9 = UUID.randomUUID()
    val profileringId10 = UUID.randomUUID()
    val periode1 = TestData.periode(
        id = periodeId1,
        identitetsnummer = fnr1
    )
    val periode2 = TestData.periode(
        id = periodeId2,
        identitetsnummer = fnr2
    )
    val periode4 = TestData.periode(
        id = periodeId4,
        identitetsnummer = fnr4
    )
    val periode5 = TestData.periode(
        id = periodeId5,
        identitetsnummer = fnr5
    )
    val periode6 = TestData.periode(
        id = periodeId6,
        identitetsnummer = fnr6
    )
    val periode8 = TestData.periode(
        id = periodeId8,
        identitetsnummer = fnr8,
        startet = TestData.metadata(
            identitetsnummer = fnr8,
            tidspunkt = prodsettingstidspunkt - Duration.ofMinutes(1)
        )
    )
    val periode9 = TestData.periode(
        id = periodeId9,
        identitetsnummer = fnr9,
        startet = TestData.metadata(
            identitetsnummer = fnr9,
            tidspunkt = prodsettingstidspunkt
        )
    )
    val periode10 = TestData.periode(
        id = periodeId10,
        identitetsnummer = fnr10,
        startet = TestData.metadata(
            identitetsnummer = fnr10,
            tidspunkt = prodsettingstidspunkt + Duration.ofMinutes(1)
        )
    )
    val profilering1 = TestData.profilering(
        id = profileringId1,
        periodeId = periodeId1,
        sendtInnAv = TestData.metadata(fnr1),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )
    val profilering2 = TestData.profilering(
        id = profileringId2,
        periodeId = periodeId2,
        sendtInnAv = TestData.metadata(fnr2),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )
    val profilering4 = TestData.profilering(
        id = profileringId4,
        periodeId = periodeId4,
        sendtInnAv = TestData.metadata(fnr4),
        profilertTil = AvroProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    )
    val profilering5 = TestData.profilering(
        id = profileringId5,
        periodeId = periodeId5,
        sendtInnAv = TestData.metadata(fnr5),
        profilertTil = AvroProfilertTil.OPPGITT_HINDRINGER
    )
    val profilering6 = TestData.profilering(
        id = profileringId6,
        periodeId = periodeId6,
        sendtInnAv = TestData.metadata(fnr6),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )
    val profilering8 = TestData.profilering(
        id = profileringId8,
        periodeId = periodeId8,
        sendtInnAv = TestData.metadata(fnr8),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )
    val profilering9 = TestData.profilering(
        id = profileringId9,
        periodeId = periodeId9,
        sendtInnAv = TestData.metadata(fnr9),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )
    val profilering10 = TestData.profilering(
        id = profileringId10,
        periodeId = periodeId10,
        sendtInnAv = TestData.metadata(fnr10),
        profilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    )

    beforeSpec {
        coEvery { kafkaKeysClientMock.getIdAndKey(fnr4.value) } returns KafkaKeysResponse(
            id = arbId4.value,
            key = key4.value
        )
        coEvery {
            kafkaKeysClientMock.getIdentiteter(dnr2.value)
        } returns IdentiteterResponse(
            identiteter = listOf(
                Identitet(identitet = dnr2.value, type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
                Identitet(identitet = fnr2.value, type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            )
        )
        coEvery {
            kafkaKeysClientMock.getIdentiteter(fnr3.value)
        } returns IdentiteterResponse(
            identiteter = emptyList()
        )
        Database.connect(dataSource)
        transaction {
            egenvurderingRepository.lagrePeriode(periode1)
            egenvurderingRepository.lagrePeriode(periode2)
            egenvurderingRepository.lagrePeriode(periode4)
            egenvurderingRepository.lagrePeriode(periode5)
            egenvurderingRepository.lagrePeriode(periode6)
            egenvurderingRepository.lagrePeriode(periode8)
            egenvurderingRepository.lagrePeriode(periode9)
            egenvurderingRepository.lagrePeriode(periode10)
            egenvurderingRepository.lagreProfilering(profilering1)
            egenvurderingRepository.lagreProfilering(profilering2)
            egenvurderingRepository.lagreProfilering(profilering4)
            egenvurderingRepository.lagreProfilering(profilering5)
            egenvurderingRepository.lagreProfilering(profilering6)
            egenvurderingRepository.lagreProfilering(profilering8)
            egenvurderingRepository.lagreProfilering(profilering9)
            egenvurderingRepository.lagreProfilering(profilering10)
        }
    }

    "Test suite for henting av egenvurdering grunnlag" - {
        "Skal returnere egenvurdering grunnlag når profilering finnes for ident" {
            val grunnlag = egenvurderingService.getEgenvurderingGrunnlag(fnr1)

            grunnlag.grunnlag.shouldNotBeNull()
            grunnlag.grunnlag.profileringId shouldBe profileringId1
            grunnlag.grunnlag.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER

            coVerify(exactly = 0) { kafkaKeysClientMock.getIdentiteter(fnr1.value) }
        }

        "Skal returnere egenvurdering grunnlag når profilering finnes for alternativ ident" {
            val grunnlag = egenvurderingService.getEgenvurderingGrunnlag(dnr2)

            grunnlag.grunnlag.shouldNotBeNull()
            grunnlag.grunnlag.profileringId shouldBe profileringId2
            grunnlag.grunnlag.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER

            coVerify(exactly = 1) { kafkaKeysClientMock.getIdentiteter(dnr2.value) }
        }


        "Skal returnere tomt grunnlag når ingen profilering finnes for identer" {
            val grunnlag = egenvurderingService.getEgenvurderingGrunnlag(fnr3)

            grunnlag.grunnlag.shouldBeNull()

            coVerify(exactly = 1) { kafkaKeysClientMock.getIdentiteter(fnr3.value) }
        }
    }

    "Test suite for innsending av egenvurdering" - {
        "Skal lagre og sender egenvurdering til Kafka" {
            val egenvurderingTopic = applicationConfig.producerConfig.egenvurderingTopic
            val request = EgenvurderingRequest(
                profileringId = profileringId4,
                egenvurdering = EgenvurdertTil.ANTATT_GODE_MULIGHETER
            )

            egenvurderingService.publiserOgLagreEgenvurdering(request, fnr4, "tokenx:Level4")

            egenvurderingKafkaProducerMock.history().size shouldBe 1
            val sentRecord = egenvurderingKafkaProducerMock.history().single()
            sentRecord.topic() shouldBe egenvurderingTopic
            sentRecord.key() shouldBe key4.value
            sentRecord.value().apply {
                this.profileringId shouldBe profilering4.id
                this.periodeId shouldBe profilering4.periodeId
                this.profilertTil shouldBe profilering4.profilertTil
                this.egenvurdering shouldBe AvroProfilertTil.ANTATT_GODE_MULIGHETER
            }

            val egenvurderingRows = egenvurderingRepository.finnEgenvurderinger(fnr4)
            egenvurderingRows shouldHaveSize 1
            val egenvurderingRow = egenvurderingRows.single()
            egenvurderingRow.profileringId shouldBe profilering4.id
            egenvurderingRow.periodeId shouldBe profilering4.periodeId
            egenvurderingRow.profilering shouldBe profilering4.profilertTil.asDto()
            egenvurderingRow.egenvurdering shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
        }

        "Skal returnere BadRequest når profilering er av ugyldig type" {
            val request = EgenvurderingRequest(
                profileringId = profileringId5,
                egenvurdering = EgenvurdertTil.ANTATT_GODE_MULIGHETER
            )

            shouldThrow<ProfileringIkkeStoettetException> {
                egenvurderingService.publiserOgLagreEgenvurdering(request, fnr5, "tokenx:Level4")
            }
        }

        "Skal returnere BadRequest når egenvurdering er av ugyldig type" {
            val request = EgenvurderingRequest(
                profileringId = profileringId6,
                egenvurdering = EgenvurdertTil.OPPGITT_HINDRINGER
            )

            shouldThrow<EgenvurderingIkkeStoettetException> {
                egenvurderingService.publiserOgLagreEgenvurdering(request, fnr6, "tokenx:Level4")
            }
        }

        "Skal returnere BadRequest når profilering ikke finnes" {
            val request = EgenvurderingRequest(
                profileringId = profileringId7,
                egenvurdering = EgenvurdertTil.ANTATT_BEHOV_FOR_VEILEDNING
            )

            shouldThrow<BadRequestException> {
                egenvurderingService.publiserOgLagreEgenvurdering(request, fnr7, "tokenx:Level4")
            }
        }
    }

    "Test suite for prodsettingstidspunkt-filter" - {


        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter før $prodsettingstidspunkt" {
            val egenvurdering = justertEgenvurderingService.getEgenvurderingGrunnlag(fnr8)

            egenvurdering.grunnlag.shouldBeNull()
        }

        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter akkurat på $prodsettingstidspunkt" {
            val egenvurdering = justertEgenvurderingService.getEgenvurderingGrunnlag(fnr9)

            egenvurdering.grunnlag.shouldBeNull()
        }

        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter etter $prodsettingstidspunkt" {
            val egenvurdering = justertEgenvurderingService.getEgenvurderingGrunnlag(fnr10)

            egenvurdering.grunnlag.shouldNotBeNull()
            egenvurdering.grunnlag.profileringId shouldBe profilering10.id
            egenvurdering.grunnlag.profilertTil shouldBe profilering10.profilertTil.asDto()
        }
    }
})

private fun mockProducer(): MockProducer<Long, AvroEgenvurdering> = MockProducer(
    true,
    RoundRobinPartitioner(),
    LongSerializer(),
    noopValueSerializer()
)

private fun noopValueSerializer(): Serializer<AvroEgenvurdering> = object : Serializer<AvroEgenvurdering> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun serialize(topic: String?, data: AvroEgenvurdering?): ByteArray = ByteArray(0)
    override fun serialize(topic: String?, headers: Headers?, data: AvroEgenvurdering?): ByteArray = ByteArray(0)
}
