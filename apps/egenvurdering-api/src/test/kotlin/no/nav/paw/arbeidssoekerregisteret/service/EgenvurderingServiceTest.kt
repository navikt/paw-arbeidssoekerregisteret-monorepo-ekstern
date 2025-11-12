package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.ProducerConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.NyesteProfilering
import no.nav.paw.arbeidssoekerregisteret.repository.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.toProfileringDto
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.Identitet
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.security.authentication.model.Claims
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.token.AccessToken
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer
import java.time.Duration
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as EgenvurderingDto

class EgenvurderingServiceTest : FreeSpec({

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val kafkaKeysClient = mockk<KafkaKeysClient>(relaxed = true)
    val producer = mockk<Producer<Long, Egenvurdering>>(relaxed = true)
    val egenvurderingRepository = mockk<EgenvurderingRepository>()

    val egenvurderingService = EgenvurderingService(
        applicationConfig = applicationConfig,
        kafkaKeysClient = kafkaKeysClient,
        producer = producer,
        egenvurderingRepository = egenvurderingRepository,
    )

    "Returnerer egenvurdering grunnlag uten å kalle kafka keys når profilering finnes for ident" {
        val ident = Identitetsnummer("10987654321")
        val profileringId = UUID.randomUUID()
        val nyesteProfilering = NyesteProfilering(
            id = profileringId,
            profilertTil = "ANTATT_GODE_MULIGHETER",
            profileringTidspunkt = Instant.now(),
            periodeStartetTidspunkt = Instant.now().minusSeconds(60)
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns nyesteProfilering

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)
        egenvurderingGrunnlag.grunnlag.shouldNotBeNull()
        egenvurderingGrunnlag.grunnlag.profileringId shouldBe profileringId
        egenvurderingGrunnlag.grunnlag.profilertTil shouldBe ANTATT_GODE_MULIGHETER

        coVerify(exactly = 0) { kafkaKeysClient.getIdentiteter(ident.verdi) }
    }

    "Returnerer egenvurdering grunnlag via alternativ ident" {
        val ident = Identitetsnummer("10987654321")
        val alternativIdent = "10987654320" // gyldig alternativ FNR
        val profileringId = UUID.randomUUID()

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns null

        coEvery {
            kafkaKeysClient.getIdentiteter(ident.verdi)
        } returns IdentiteterResponse(
            identiteter = listOf(
                Identitet(identitet = ident.verdi, type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
                Identitet(identitet = alternativIdent, type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            )
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(
                Identitetsnummer(
                    alternativIdent
                )
            )
        } returns NyesteProfilering(
            id = profileringId,
            profilertTil = "ANTATT_GODE_MULIGHETER",
            profileringTidspunkt = Instant.now(),
            periodeStartetTidspunkt = Instant.now().minusSeconds(60)
        )

        val grunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)

        grunnlag.grunnlag.shouldNotBeNull()
        grunnlag.grunnlag.profileringId shouldBe profileringId
        grunnlag.grunnlag.profilertTil shouldBe ANTATT_GODE_MULIGHETER

        coVerify(exactly = 1) { kafkaKeysClient.getIdentiteter(ident.verdi) }
    }


    "Tomt grunnlag når ingen profilering finnes for identer" {
        val ident = Identitetsnummer("12345678901")

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns null

        coEvery {
            kafkaKeysClient.getIdentiteter(ident.verdi)
        } returns IdentiteterResponse(identiteter = emptyList())

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)
        egenvurderingGrunnlag.grunnlag.shouldBeNull()

        coVerify(exactly = 1) { kafkaKeysClient.getIdentiteter(ident.verdi) }
    }

    "IllegalArgumentException når profilertTil ikke kan mappes" {
        val ident = Identitetsnummer("55555555555")
        val profileringId = UUID.randomUUID()
        val ugyldig = NyesteProfilering(
            id = profileringId,
            profilertTil = "NOPE_NOPE_NOPE",
            profileringTidspunkt = Instant.now(),
            periodeStartetTidspunkt = Instant.now().minusSeconds(60)
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns ugyldig

        shouldThrow<IllegalArgumentException> {
            egenvurderingService.getEgenvurderingGrunnlag(ident)
        }
    }

    "Lagrer og sender egenvurdering til Kafka" {
        val applicationConfig = mockk<ApplicationConfig>()
        val egenvurderingTopic = "egenvurdering-topic"
        every { applicationConfig.producerConfig } returns ProducerConfig("v1", "app", egenvurderingTopic)

        val kafkaKeysClient = mockk<KafkaKeysClient>()
        val kafkaKey = 42L
        coEvery { kafkaKeysClient.getIdAndKey(any()) } returns KafkaKeysResponse(id = 1L, key = kafkaKey)

        val profileringId = UUID.randomUUID()
        val periodeId = UUID.randomUUID()

        val repo = mockk<EgenvurderingRepository>(relaxed = true)
        every { repo.finnProfilering(profileringId, any()) } returns ProfileringRow(
            id = profileringId,
            periodeId = periodeId,
            profilertTil = "ANTATT_BEHOV_FOR_VEILEDNING"
        )
        every { repo.lagreEgenvurdering(any()) } just Runs

        val mockProducer = mockProducer()

        val service = EgenvurderingService(
            applicationConfig = applicationConfig,
            kafkaKeysClient = kafkaKeysClient,
            producer = mockProducer,
            egenvurderingRepository = repo
        )

        val securityContext = SecurityContext(
            bruker = Sluttbruker(Identitetsnummer("10987654321"), sikkerhetsnivaa = "tokenx:Level4"),
            accessToken = AccessToken(jwt = "dummy", issuer = TokenX, claims = Claims(emptyMap()))
        )

        val request = EgenvurderingRequest(
            profileringId = profileringId,
            egenvurdering = EgenvurderingDto.OPPGITT_HINDRINGER
        )

        service.publiserOgLagreEgenvurdering(request, securityContext)

        mockProducer.history().size shouldBe 1
        val sentRecord = mockProducer.history().single()
        sentRecord.topic() shouldBe egenvurderingTopic
        sentRecord.key() shouldBe kafkaKey
        sentRecord.value().apply {
            this.profileringId shouldBe profileringId
            this.periodeId shouldBe periodeId
            this.profilertTil.name shouldBe "ANTATT_BEHOV_FOR_VEILEDNING"
            this.egenvurdering.name shouldBe EgenvurderingDto.OPPGITT_HINDRINGER.name
        }

        verify(exactly = 1) { repo.lagreEgenvurdering(any()) }
    }

    "BadRequest når profilering ikke finnes" {
        val profileringId = UUID.randomUUID()
        val request = EgenvurderingRequest(
            profileringId = profileringId,
            egenvurdering = EgenvurderingDto.ANTATT_BEHOV_FOR_VEILEDNING
        )

        every { egenvurderingRepository.finnProfilering(profileringId, any()) } returns null
        coEvery { kafkaKeysClient.getIdAndKey(any()) } returns KafkaKeysResponse(id = 1L, key = 42L)

        val sluttbrukerIdent = Identitetsnummer("10987654321")
        val sluttbruker = Sluttbruker(sluttbrukerIdent, sikkerhetsnivaa = "tokenx:Level4")
        val securityContext = SecurityContext(
            bruker = sluttbruker,
            accessToken = AccessToken(
                jwt = "dummy",
                issuer = TokenX,
                claims = Claims(emptyMap())
            )
        )

        shouldThrow<BadRequestException> {
            egenvurderingService.publiserOgLagreEgenvurdering(request, securityContext)
        }
        verify(exactly = 0) { egenvurderingRepository.lagreEgenvurdering(any()) }
    }

    "Tester for deprekeringstidspunkt-filter" - {
        val deprekertTidspunkt = Instant.now().minusSeconds(60)
        val justerApplicationConfig = applicationConfig.copy(
            deprekeringstidspunktBehovsvurdering = deprekertTidspunkt
        )
        val justertEgenvurderingService = EgenvurderingService(
            applicationConfig = justerApplicationConfig,
            kafkaKeysClient = kafkaKeysClient,
            producer = producer,
            egenvurderingRepository = egenvurderingRepository,
        )

        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter før $deprekertTidspunkt" {
            val profilering = NyesteProfilering(
                id = UUID.randomUUID(),
                profilertTil = ProfilertTil.OPPGITT_HINDRINGER.name,
                profileringTidspunkt = Instant.now(),
                periodeStartetTidspunkt = deprekertTidspunkt - Duration.ofMinutes(
                    1
                )
            )

            every {
                egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(any())
            } returns profilering

            val egenvurdering = justertEgenvurderingService
                .getEgenvurderingGrunnlag(Identitetsnummer("01017012345"))

            egenvurdering.grunnlag.shouldBeNull()
        }

        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter akkurat $deprekertTidspunkt" {
            val profilering = NyesteProfilering(
                id = UUID.randomUUID(),
                profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER.name,
                profileringTidspunkt = Instant.now(),
                periodeStartetTidspunkt = deprekertTidspunkt
            )

            every {
                egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(any())
            } returns profilering

            val egenvurdering = justertEgenvurderingService
                .getEgenvurderingGrunnlag(Identitetsnummer("01017012345"))

            egenvurdering.grunnlag shouldBe profilering.toProfileringDto()
        }

        "Skal returnere tomt egenvurderingsgrunnlag for perioder som starter etter $deprekertTidspunkt" {
            val profilering = NyesteProfilering(
                id = UUID.randomUUID(),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING.name,
                profileringTidspunkt = Instant.now(),
                periodeStartetTidspunkt = deprekertTidspunkt + Duration.ofMinutes(1)
            )

            every {
                egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(any())
            } returns profilering

            val egenvurdering = justertEgenvurderingService
                .getEgenvurderingGrunnlag(Identitetsnummer("01017012345"))

            egenvurdering.grunnlag shouldBe profilering.toProfileringDto()
        }
    }
})

private fun mockProducer(): MockProducer<Long, Egenvurdering> = MockProducer(
    true,
    RoundRobinPartitioner(),
    LongSerializer(),
    noopValueSerializer()
)

private fun noopValueSerializer(): Serializer<Egenvurdering> = object : Serializer<Egenvurdering> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun serialize(topic: String?, data: Egenvurdering?): ByteArray = ByteArray(0)
    override fun serialize(topic: String?, headers: Headers?, data: Egenvurdering?): ByteArray = ByteArray(0)
}
