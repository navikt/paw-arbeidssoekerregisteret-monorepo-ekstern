package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.opplysninger
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.arena.adapter.HOEYVANNSMERKE
import java.time.Duration
import java.time.Instant
import java.util.*


class TidsFilterTopologyTest : FreeSpec({
    with(testScope()) {
        val fixedKey = 1L
        "Når periode startes og stoppes før $HOEYVANNSMERKE skal vi ikke få noe ut på arena topic" {
            val periodeStart = periode(
                identietsnummer = "12345678901",
                startet = metadata(timestamp = HOEYVANNSMERKE.minus(8.days)),
            )
            periodeTopic.pipeInput(fixedKey, periodeStart)
            val opplysninger = opplysninger(periode = periodeStart.id, timestamp = HOEYVANNSMERKE.minus(8.days))
            profileringsTopic.pipeInput(
                fixedKey,
                profilering(periode = periodeStart.id, timestamp = HOEYVANNSMERKE.minus(8.days)),
                HOEYVANNSMERKE.minus(8.days)
            )
            periodeTopic.pipeInput(periodeStart.avslutt(HOEYVANNSMERKE.minus(4.days)))
            arenaTopic.isEmpty shouldBe true
            joinStore.get(periodeStart.id).shouldBeNull()
        }
        "Når periode startes før $HOEYVANNSMERKE og stoppes etter $HOEYVANNSMERKE skal vi få ut stopp melding på arena topic" {
            val periodeStart = periode(
                identietsnummer = "12345678901",
                startet = metadata(timestamp = HOEYVANNSMERKE.minus(8.days)),
            )
            periodeTopic.pipeInput(fixedKey, periodeStart)
            periodeTopic.pipeInput(periodeStart.avslutt(HOEYVANNSMERKE.plus(4.days)))
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readValue().periode?.avsluttet.shouldNotBeNull()
            joinStore.get(periodeStart.id).shouldBeNull()
        }
        "Når periode startes før $HOEYVANNSMERKE og stoppes etter $HOEYVANNSMERKE skal vi ikke inkludere opplysninger eller profilering i stopp meldingen" {
            val periodeStart = periode(
                identietsnummer = "12345678901",
                startet = metadata(timestamp = HOEYVANNSMERKE.minus(8.days)),
            )
            val opplysninger = opplysninger(
                periode = periodeStart.id,
                timestamp = HOEYVANNSMERKE.plus(4.days)
            )
            profileringsTopic.pipeInput(
                fixedKey, profilering(
                    opplysningerId = opplysninger.id,
                    periode = periodeStart.id,
                    timestamp = HOEYVANNSMERKE.plus(4.days)
                )
            )
            periodeTopic.pipeInput(fixedKey, periodeStart)
            periodeTopic.pipeInput(periodeStart.avslutt(HOEYVANNSMERKE.plus(6.days)))
            arenaTopic.isEmpty shouldBe false
            with(arenaTopic.readValue()) {
                periode?.startet.shouldNotBeNull()
                periode?.avsluttet.shouldNotBeNull()
                opplysningerOmArbeidssoeker.shouldBeNull()
                profilering.shouldBeNull()
            }
            joinStore.get(periodeStart.id).shouldBeNull()
        }
        "Når periode startes og stoppes etter $HOEYVANNSMERKE skal vi inkludere opplysninger og profilering i stopp meldingen" - {
            val periodeStart = periode(
                identietsnummer = "12345678901",
                startet = metadata(timestamp = HOEYVANNSMERKE.plus(1.days)),
            )
            "Verifiser tom arena topic og ingen data i join store" {
                arenaTopic.isEmpty shouldBe true
                joinStore.get(periodeStart.id).shouldBeNull()
            }
            "Når periode og profilering er sendt inn skal vi få ut en komplett arena melding" {
                periodeTopic.pipeInput(fixedKey, periodeStart)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue().periode.shouldNotBeNull()
                profileringsTopic.pipeInput(
                    fixedKey, profilering(
                        opplysningerId = UUID.randomUUID(),
                        periode = periodeStart.id,
                        timestamp = HOEYVANNSMERKE.plus(4.days)
                    )
                )
                arenaTopic.isEmpty shouldBe false
                with(arenaTopic.readValue()) {
                    periode?.startet.shouldNotBeNull()
                    periode?.avsluttet.shouldBeNull()
                    opplysningerOmArbeidssoeker.shouldBeNull()
                    profilering.shouldNotBeNull()
                }
            }
            "Når periode avsluttes skal vi få ut en melding med bare periode" {
                periodeTopic.pipeInput(fixedKey, periodeStart)
                periodeTopic.pipeInput(periodeStart.avslutt(HOEYVANNSMERKE.plus(6.days)))
                arenaTopic.isEmpty shouldBe false
                with(arenaTopic.readValue()) {
                    periode?.startet.shouldNotBeNull()
                    periode?.avsluttet.shouldNotBeNull()
                    profilering.shouldBeNull()
                    opplysningerOmArbeidssoeker.shouldBeNull()
                }
                joinStore.get(periodeStart.id).shouldBeNull()
            }
        }
    }
})

val Int.days: Duration get() = Duration.ofDays(toLong())

fun Periode.avslutt(timestamp: Instant) =
    periode(
        id = id,
        identietsnummer = identitetsnummer,
        startet = startet,
        avsluttet = metadata(timestamp = timestamp)
    )