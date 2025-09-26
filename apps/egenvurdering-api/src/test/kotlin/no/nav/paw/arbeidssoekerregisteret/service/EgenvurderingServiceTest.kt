package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.ProducerConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.NyesteProfilering
import no.nav.paw.arbeidssoekerregisteret.repository.ProfileringRow
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as EgenvurderingDto
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.model.Identitetsnummer
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
import java.time.Instant
import java.util.*

class EgenvurderingServiceTest : FreeSpec({

    val applicationConfig = mockk<ApplicationConfig>(relaxed = true)
    val kafkaKeysClient = mockk<KafkaKeysClient>(relaxed = true)
    val producer = mockk<Producer<Long, Egenvurdering>>(relaxed = true)
    val egenvurderingRepository = mockk<EgenvurderingRepository>()

    val egenvurderingService = EgenvurderingService(
        applicationConfig = applicationConfig,
        kafkaKeysClient = kafkaKeysClient,
        producer = producer,
        egenvurderingRepository = egenvurderingRepository,
    )


    "Returnerer egenvurdering grunnlag" {
        val ident = Identitetsnummer("10987654321")
        val profileringId = UUID.randomUUID()
        val nyesteProfilering = NyesteProfilering(
            id = profileringId,
            profilertTil = "ANTATT_GODE_MULIGHETER",
            tidspunkt = Instant.now()
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns nyesteProfilering

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)
        egenvurderingGrunnlag.grunnlag.shouldNotBeNull()
        egenvurderingGrunnlag.grunnlag.profileringId shouldBe profileringId
        egenvurderingGrunnlag.grunnlag.profilertTil shouldBe ANTATT_GODE_MULIGHETER
    }

    "Tomt grunnlag når repository ikke finner profilering" {
        val ident = Identitetsnummer("12345678901")

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns null

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(ident)
        egenvurderingGrunnlag.grunnlag.shouldBeNull()
    }


    "IllegalArgumentException når profilertTil ikke kan mappes" {
        val ident = Identitetsnummer("55555555555")
        val profileringId = UUID.randomUUID()
        val ugyldig = NyesteProfilering(
            id = profileringId,
            profilertTil = "NOPE_NOPE_NOPE",
            tidspunkt = Instant.now()
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns ugyldig

        shouldThrow<IllegalArgumentException> {
            egenvurderingService.getEgenvurderingGrunnlag(ident)
        }
    }

    "postEgenvurdering - sender melding til Kafka (MockProducer)" {
        val applicationConfig = mockk<ApplicationConfig>()
        val egenvurderingTopic = "egenvurdering-topic"
        every { applicationConfig.producerConfig } returns ProducerConfig("v1", "app", egenvurderingTopic)

        val kafkaKeysClient = mockk<KafkaKeysClient>()
        val kafkaKey = 42L
        coEvery { kafkaKeysClient.getIdAndKey(any()) } returns KafkaKeysResponse(id = 1L, key = kafkaKey)

        val profileringId = UUID.randomUUID()
        val periodeId = UUID.randomUUID()

        val repo = mockk<EgenvurderingRepository>()
        every { repo.finnProfilering(profileringId, any()) } returns ProfileringRow(
            id = profileringId,
            periodeId = periodeId,
            profilertTil = "ANTATT_BEHOV_FOR_VEILEDNING"
        )

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

        service.postEgenvurdering(request, securityContext)

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
    }

    "postEgenvurdering - kaster BadRequest når profilering ikke finnes" {
        val topic = "egenvurdering-topic"
        every { applicationConfig.producerConfig } returns ProducerConfig("v1", "app", topic)

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
            egenvurderingService.postEgenvurdering(request, securityContext)
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
