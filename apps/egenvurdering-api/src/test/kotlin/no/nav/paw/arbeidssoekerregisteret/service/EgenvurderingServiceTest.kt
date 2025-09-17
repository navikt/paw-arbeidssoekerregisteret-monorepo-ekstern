package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.NyesteProfilering
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.texas.TexasClient
import org.apache.kafka.clients.producer.Producer
import java.time.Instant
import java.util.*

class EgenvurderingServiceTest : FreeSpec({

    val applicationConfig = mockk<ApplicationConfig>(relaxed = true)
    val kafkaKeysClient = mockk<KafkaKeysClient>(relaxed = true)
    val producer = mockk<Producer<Long, Egenvurdering>>(relaxed = true)
    val texasClient = mockk<TexasClient>(relaxed = true)
    val oppslagsClient = mockk<ApiOppslagClient>(relaxed = true)
    val egenvurderingRepository = mockk<EgenvurderingRepository>()

    val egenvurderingService = EgenvurderingService(
        applicationConfig = applicationConfig,
        kafkaKeysClient = kafkaKeysClient,
        producer = producer,
        texasClient = texasClient,
        oppslagsClient = oppslagsClient,
        egenvurderingRepository = egenvurderingRepository,
    )


    "Returnerer egenvurdering grunnlag" {
        val ident = "10987654321"
        val securityContext = securityContextMed(ident)
        val profileringId = UUID.randomUUID()
        val nyesteProfilering = NyesteProfilering(
            id = profileringId,
            profilertTil = "ANTATT_GODE_MULIGHETER",
            tidspunkt = Instant.now()
        )

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns nyesteProfilering

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(securityContext)
        egenvurderingGrunnlag.grunnlag.shouldNotBeNull()
        egenvurderingGrunnlag.grunnlag.profileringId shouldBe profileringId
        egenvurderingGrunnlag.grunnlag.profilertTil shouldBe ANTATT_GODE_MULIGHETER
    }

    "Tomt grunnlag når repository ikke finner profilering" {
        val ident = "12345678901"
        val securityContext = securityContextMed(ident)

        every {
            egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        } returns null

        val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(securityContext)
        egenvurderingGrunnlag.grunnlag.shouldBeNull()
    }


    "IllegalArgumentException når profilertTil ikke kan mappes" {
        val ident = "55555555555"
        val securityContext = securityContextMed(ident)
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
            egenvurderingService.getEgenvurderingGrunnlag(securityContext)
        }
    }
})

private fun securityContextMed(ident: String): SecurityContext =
    mockk(relaxed = true) {
        every { bruker.ident.toString() } returns ident
    }
