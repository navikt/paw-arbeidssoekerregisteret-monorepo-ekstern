package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class EnkelTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Verifiser standard flyt" - {
            val periode = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678901",
                startet = metadata(Instant.parse("2023-12-01T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foerstInnsendteOpplysninger = periode.key to opplysninger(
                periode = periode.melding.id,
                timestamp = Instant.parse("2023-12-30T00:00:00Z")
            )
            "Vi ignorer opplysninger sendt inn før 1. januar 2024" {
                opplysningerTopic.pipeInput(foerstInnsendteOpplysninger.key, foerstInnsendteOpplysninger.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foeerstInnsendteOpplysningerI2024 = periode.key to opplysninger(
                periode = periode.melding.id,
                timestamp = Instant.parse("2024-01-01T00:00:01Z")
            )
            "Når nye opplysningene blir tilgjengelig i 20204 skal vi ikke få noe ut på arena topic" {
                opplysningerTopic.pipeInput(
                    foeerstInnsendteOpplysningerI2024.key,
                    foeerstInnsendteOpplysningerI2024.melding
                )
                arenaTopic.isEmpty shouldBe true
            }
            val oppdaterteOpplysningerI2024 = foerstInnsendteOpplysninger.key to opplysninger(
                periode = foerstInnsendteOpplysninger.melding.periodeId,
                timestamp = Instant.parse("2024-01-02T00:00:02Z")
            )
            "Når opplysningene blir oppdatert skal vi ikke få ut noe på arena topic" {
                opplysningerTopic.pipeInput(oppdaterteOpplysningerI2024.key, oppdaterteOpplysningerI2024.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val profileringAvFoertsteOpplysningerI2024 = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = foeerstInnsendteOpplysningerI2024.melding.id,
                periode = foeerstInnsendteOpplysningerI2024.melding.periodeId,
                timestamp = Instant.parse("2024-01-04T00:00:03Z")
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi få periode, første opplysninger fra 2024 og profilering ut på arena topic" {
                profileringsTopic.pipeInput(
                    profileringAvFoertsteOpplysningerI2024.key,
                    profileringAvFoertsteOpplysningerI2024.melding
                )
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe periode.key
                assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                assertApiOpplysningerMatchesArenaOpplysninger(
                    foeerstInnsendteOpplysningerI2024.melding,
                    arenaTilstand.opplysningerOmArbeidssoeker
                )
                assertApiProfileringMatchesArenaProfilering(
                    profileringAvFoertsteOpplysningerI2024.melding,
                    arenaTilstand.profilering
                )
            }
            val profileringForOppdaterteOpplysningerI2024 = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = oppdaterteOpplysningerI2024.melding.id,
                periode = oppdaterteOpplysningerI2024.melding.periodeId
            )
            "Når profileringen for oppdaterte opplysninger ankommer og periode og opplysninger er tilgjengelig vi ikke få ut noe" {
                profileringsTopic.pipeInput(
                    profileringForOppdaterteOpplysningerI2024.key,
                    profileringForOppdaterteOpplysningerI2024.melding
                )
                arenaTopic.isEmpty shouldBe true
            }
            "Når perioden avsluttes skal vi få ut ny melding med avsluttet periode og opprinnelige opplysninger med tilhørende profilering" {
                val avsluttetPeriode = periode.key to periode(
                    id = periode.melding.id,
                    identietsnummer = periode.melding.identitetsnummer,
                    startet = periode.melding.startet,
                    avsluttet = metadata(
                        periode.melding.startet.tidspunkt + java.time.Duration.ofDays(30)
                    )
                )
                periodeTopic.pipeInput(avsluttetPeriode.key, avsluttetPeriode.melding)
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe avsluttetPeriode.key
                assertApiPeriodeMatchesArenaPeriode(
                    api = avsluttetPeriode.melding,
                    arena = arenaTilstand.periode
                )
                assertApiOpplysningerMatchesArenaOpplysninger(
                    api = foeerstInnsendteOpplysningerI2024.melding,
                    arena = arenaTilstand.opplysningerOmArbeidssoeker
                )
                assertApiProfileringMatchesArenaProfilering(
                    api = profileringAvFoertsteOpplysningerI2024.melding,
                    arena = arenaTilstand.profilering
                )
            }
        }
    }
})

