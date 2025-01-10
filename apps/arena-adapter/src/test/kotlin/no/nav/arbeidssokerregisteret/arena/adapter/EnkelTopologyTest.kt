package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.assertApiOpplysningerMatchesArenaOpplysninger
import no.nav.arbeidssokerregisteret.arena.adapter.utils.assertApiPeriodeMatchesArenaPeriode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.assertApiProfileringMatchesArenaProfilering
import no.nav.arbeidssokerregisteret.arena.adapter.utils.key
import no.nav.arbeidssokerregisteret.arena.adapter.utils.melding
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.opplysninger
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class EnkelTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Verifiser standard flyt" - {
            val periode = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678901",
                startet = metadata(Instant.parse("2024-01-02T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foerstInnsendteOpplysninger = periode.key to opplysninger(
                periode = periode.melding.id,
                timestamp = Instant.parse("2024-01-01T00:00:01Z")
            )
            "Når nye opplysningene blir tilgjengelig skal vi ikke få noe ut på arena topic" {
                opplysningerTopic.pipeInput(
                    foerstInnsendteOpplysninger.key,
                    foerstInnsendteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe true
            }
            val oppdaterteOpplysninger = foerstInnsendteOpplysninger.key to opplysninger(
                periode = foerstInnsendteOpplysninger.melding.periodeId,
                timestamp = Instant.parse("2024-01-02T00:00:02Z")
            )
            "Når opplysningene blir oppdatert skal vi ikke få ut noe på arena topic" {
                opplysningerTopic.pipeInput(oppdaterteOpplysninger.key, oppdaterteOpplysninger.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val profileringAvFoertsteOpplysninger = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = foerstInnsendteOpplysninger.melding.id,
                periode = foerstInnsendteOpplysninger.melding.periodeId,
                timestamp = Instant.parse("2024-01-02T00:00:03Z")
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi få periode, første opplysninger fra 2024 og profilering ut på arena topic" {
                profileringsTopic.pipeInput(
                    profileringAvFoertsteOpplysninger.key,
                    profileringAvFoertsteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe periode.key
                assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                assertApiOpplysningerMatchesArenaOpplysninger(
                    foerstInnsendteOpplysninger.melding,
                    arenaTilstand.opplysningerOmArbeidssoeker
                )
                assertApiProfileringMatchesArenaProfilering(
                    profileringAvFoertsteOpplysninger.melding,
                    arenaTilstand.profilering
                )
            }
            val profileringForOppdaterteOpplysninger = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = oppdaterteOpplysninger.melding.id,
                periode = oppdaterteOpplysninger.melding.periodeId
            )
            "Når profileringen for oppdaterte opplysninger ankommer og periode og opplysninger er tilgjengelig vil vi ikke få ut noe" {
                profileringsTopic.pipeInput(
                    profileringForOppdaterteOpplysninger.key,
                    profileringForOppdaterteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe true
            }
            "Når perioden startet etter 2024 avsluttes skal vi få ut ny melding med avsluttet periode uten profilering og opplysning" {
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
                arenaTilstand.opplysningerOmArbeidssoeker shouldBe null
                arenaTilstand.profilering shouldBe null
            }
        }
        "Startet før 2024" - {
            val periodeFoer2024 = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678902",
                startet = metadata(Instant.parse("2023-12-01T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periodeFoer2024.key, periodeFoer2024.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val InnsendteOpplysningerFoer2024 = periodeFoer2024.key to opplysninger(
                periode = periodeFoer2024.melding.id,
                timestamp = Instant.parse("2023-12-01T00:00:00Z")
            )
            "Vi ignorer opplysninger sendt inn før 1. januar 2024" {
                opplysningerTopic.pipeInput(InnsendteOpplysningerFoer2024.key, InnsendteOpplysningerFoer2024.melding)
                arenaTopic.isEmpty shouldBe true
            }
            "Vi ignorer profileringer for opplysninger sendt inn før 1. januar 2024(skal normalt sett ikke dukke opp)" {
                profileringsTopic.pipeInput(
                    InnsendteOpplysningerFoer2024.key,
                    profilering(
                        opplysningerId = InnsendteOpplysningerFoer2024.melding.id,
                        periode = InnsendteOpplysningerFoer2024.melding.periodeId
                    ),
                    InnsendteOpplysningerFoer2024.second.sendtInnAv.tidspunkt.toEpochMilli()
                )
                arenaTopic.isEmpty shouldBe true
            }

            "Når perioden avsluttes skal vi få ut ny melding med avsluttet periode og null for profilering og opplysning" {
                val avsluttetPeriode = periodeFoer2024.key to periode(
                    id = periodeFoer2024.melding.id,
                    identietsnummer = periodeFoer2024.melding.identitetsnummer,
                    startet = periodeFoer2024.melding.startet,
                    avsluttet = metadata(
                        periodeFoer2024.melding.startet.tidspunkt + java.time.Duration.ofDays(60)
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
                arenaTilstand.opplysningerOmArbeidssoeker shouldBe null
                arenaTilstand.profilering shouldBe null
            }

            "Når perioden startes og avsluttes før 2024 vi ikke få ut noe på arenatopicet" {
                val avsluttetPeriodeFoer2024 = periodeFoer2024.key to periode(
                    id = periodeFoer2024.melding.id,
                    identietsnummer = periodeFoer2024.melding.identitetsnummer,
                    startet = periodeFoer2024.melding.startet,
                    avsluttet = metadata(
                        periodeFoer2024.melding.startet.tidspunkt + java.time.Duration.ofDays(30)
                    )
                )
                periodeTopic.pipeInput(periodeFoer2024.key, periodeFoer2024.melding)
                periodeTopic.pipeInput(avsluttetPeriodeFoer2024.key, avsluttetPeriodeFoer2024.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foerstInnsendteOpplysningerI2024 = periodeFoer2024.key to opplysninger(
                periode = periodeFoer2024.melding.id,
                timestamp = Instant.parse("2024-01-02T00:00:01Z")
            )
            "Når nye opplysningene blir tilgjengelig i 20204 skal vi ikke få noe ut på arena topic" {
                opplysningerTopic.pipeInput(
                    foerstInnsendteOpplysningerI2024.key,
                    foerstInnsendteOpplysningerI2024.melding
                )
                arenaTopic.isEmpty shouldBe true
            }

            val profileringAvOpplysningerI2024 = InnsendteOpplysningerFoer2024.key to profilering(
                opplysningerId = InnsendteOpplysningerFoer2024.melding.id,
                periode = InnsendteOpplysningerFoer2024.melding.periodeId,
                timestamp = Instant.parse("2024-01-04T00:00:03Z")
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi fremdeles ikke få noe ut på arena topicet" {
                profileringsTopic.pipeInput(
                    profileringAvOpplysningerI2024.key,
                    profileringAvOpplysningerI2024.melding
                )
                arenaTopic.isEmpty shouldBe true
            }

        }
    }
})

