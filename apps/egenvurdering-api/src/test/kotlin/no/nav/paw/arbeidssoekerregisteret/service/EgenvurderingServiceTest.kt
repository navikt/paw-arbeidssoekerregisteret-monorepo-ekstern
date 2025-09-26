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
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.texas.TexasClient
import org.apache.kafka.clients.producer.Producer
import java.time.Instant
import java.util.*

class EgenvurderingServiceTest : FreeSpec({

    val applicationConfig = mockk<ApplicationConfig>(relaxed = true)
    val kafkaKeysClient = mockk<KafkaKeysClient>(relaxed = true)
    val producer = mockk<Producer<Long, Egenvurdering>>(relaxed = true)
    val texasClient = mockk<TexasClient>(relaxed = true)
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
})
