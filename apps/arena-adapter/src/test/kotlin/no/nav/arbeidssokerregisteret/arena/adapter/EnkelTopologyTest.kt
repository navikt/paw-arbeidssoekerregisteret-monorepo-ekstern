package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
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
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class EnkelTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Verifiser standard flyt" - {
            "Når profileringen kommer midre enn 5 sekunder etter perioden får vi en melding med begge deler" {
                val testPeriode = keySequence.incrementAndGet() to periode(
                    identietsnummer = "09876543211",
                    startet = metadata(Instant.parse("2025-03-04T12:00:00Z"))
                )
                periodeTopic.pipeInput(testPeriode.key, testPeriode.melding)
                arenaTopic.isEmpty shouldBe true
                topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(3))
                arenaTopic.isEmpty shouldBe true
                profileringsTopic.pipeInput(testPeriode.key, profilering(
                    opplysningerId = UUID.randomUUID(),
                    periode = testPeriode.melding.id,
                    timestamp = Instant.parse("2025-03-04T12:00:03Z")
                ))
                arenaTopic.isEmpty shouldBe false
                val kv = arenaTopic.readKeyValue()
                kv.key shouldBe testPeriode.key
                kv.value should { arenaMelding ->
                    val periode = arenaMelding.periode.shouldNotBeNull()
                    val profilering = arenaMelding.profilering.shouldNotBeNull()
                    periode.id shouldBe testPeriode.melding.id
                    profilering.periodeId shouldBe testPeriode.melding.id
                }
                topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
                arenaTopic.isEmpty shouldBe true
            }

            val periode = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678901",
                startet = metadata(Instant.parse("2024-01-02T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi få melding på arena topic med bare periode etter 5 sekunder" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
                topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(3))
                arenaTopic.isEmpty shouldBe true
                topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(2))
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readKeyValue() should { kv ->
                    kv.key shouldBe periode.key
                    kv.value.periode.id shouldBe periode.melding.id
                    kv.value.periode.startet.tidspunkt shouldBe periode.melding.startet.tidspunkt
                    kv.value.periode.startet.utfoertAv.id shouldBe periode.melding.startet.utfoertAv.id
                    kv.value.periode.startet.utfoertAv.type.name shouldBe periode.melding.startet.utfoertAv.type.name
                }
            }
            val profileringAvFoertsteOpplysninger = periode.key to profilering(
                opplysningerId = UUID.randomUUID(),
                periode = periode.melding.id,
                timestamp = Instant.parse("2024-01-02T00:00:03Z")
            )
            "Når profileringen ankommer og periode er tilgjengelig skal vi få periode, første opplysninger fra 2024 og profilering ut på arena topic" {
                profileringsTopic.pipeInput(
                    profileringAvFoertsteOpplysninger.key,
                    profileringAvFoertsteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe periode.key
                assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                assertApiProfileringMatchesArenaProfilering(
                    profileringAvFoertsteOpplysninger.melding,
                    arenaTilstand.profilering
                )
            }
            val profileringForOppdaterteOpplysninger = periode.key to profilering(
                opplysningerId = UUID.randomUUID(),
                periode = periode.melding.id
            )
            "Når profileringen for oppdaterte opplysninger ankommer og periode er tilgjengelig vil vi ikke få ut noe" {
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

            "Vi ignorer profileringer for opplysninger sendt inn før 1. januar 2024(skal normalt sett ikke dukke opp)" {
                profileringsTopic.pipeInput(
                    periodeFoer2024.key,
                    profilering(
                        opplysningerId = UUID.randomUUID(),
                        periode = periodeFoer2024.melding.id
                    ),
                    periodeFoer2024.melding.startet.tidspunkt.toEpochMilli()
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

            val profileringAvOpplysningerI2024 = periodeFoer2024.key to profilering(
                opplysningerId = UUID.randomUUID(),
                periode = periodeFoer2024.melding.id,
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

