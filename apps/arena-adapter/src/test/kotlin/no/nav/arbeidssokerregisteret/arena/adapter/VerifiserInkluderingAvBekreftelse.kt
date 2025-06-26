package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import no.nav.paw.arbeidssokerregisteret.arena.adapter.bekreftelseForsinkelseFoerRydding
import no.nav.paw.arbeidssokerregisteret.arena.adapter.bekreftelseHoeyvannsmerke
import no.nav.paw.arbeidssokerregisteret.arena.adapter.bekreftelseRyddigIntervall
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import java.time.Duration


class VerifiserInkluderingAvBekreftelse : FreeSpec({
    val tiSekunder = Duration.ofSeconds(10)
    val ettSekund = Duration.ofSeconds(1)
    with(testScope(initialWallClockTime = bekreftelseHoeyvannsmerke)) {
        "Når bekreftelse med svar 'nei' mottas før $bekreftelseHoeyvannsmerke skal den ikke lagres" {
            val bekreftelse = bekreftelseMelding(tidspunkt = bekreftelseHoeyvannsmerke - 1.dager, vilFortsetteSomArbeidssoeker = false)
            bekreftelseTopic.pipeInput(1L, bekreftelse)
            bekreftelseStore.get(bekreftelse.periodeId) shouldBe null
        }
        "Når bekreftelse med svar 'nei' mottas etter $bekreftelseHoeyvannsmerke skal den lagres i  $bekreftelseForsinkelseFoerRydding" - {
            val bekreftelse = bekreftelseMelding(tidspunkt = bekreftelseHoeyvannsmerke + ettSekund, vilFortsetteSomArbeidssoeker = false)
            bekreftelseTopic.pipeInput(1L, bekreftelse)
            "Bekreftelsen er laget" {
                bekreftelseStore.get(bekreftelse.periodeId) shouldBe bekreftelse
            }
            "Bekreftelsen er slettet når $bekreftelseForsinkelseFoerRydding har gått siden innsending" {
                this@with.topologyTestDriver.advanceWallClockTime(bekreftelseForsinkelseFoerRydding + bekreftelseRyddigIntervall + tiSekunder)
                bekreftelseStore.get(bekreftelse.periodeId) shouldBe null
            }
        }
        "Når bekreftelse med svar 'ja' mottas etter $bekreftelseHoeyvannsmerke skal den ikke lagres" {
            val bekreftelse = bekreftelseMelding(tidspunkt = bekreftelseHoeyvannsmerke + ettSekund, vilFortsetteSomArbeidssoeker = true)
            bekreftelseTopic.pipeInput(1L, bekreftelse)
            bekreftelseStore.get(bekreftelse.periodeId) shouldBe null
        }
        "Verifiser standard flyt" - {
            val periodeStart = periode("12345678901", startet = metadata(timestamp = bekreftelseHoeyvannsmerke))
            val profilering = profilering(periode = periodeStart.id, timestamp = bekreftelseHoeyvannsmerke + tiSekunder)
            val bekreftelseTidspunkt = bekreftelseHoeyvannsmerke + 1.dager
            val periodeStopp = periodeStart.avslutt(bekreftelseTidspunkt + ettSekund)
            "Når periode avsluttes etter svar 'nei' skal bekreftelsen inkluderes i arena-tilstanden" {
                val bekreftelse = bekreftelseMelding(
                    periodeId = periodeStart.id,
                    tidspunkt = bekreftelseTidspunkt,
                    vilFortsetteSomArbeidssoeker = false
                )
                bekreftelseStore.get(bekreftelse.periodeId) shouldBe null
                periodeTopic.pipeInput(1L, periodeStart)
                this@with.topologyTestDriver.advanceWallClockTime(ettSekund)
                arenaTopic.isEmpty shouldBe true
                profileringsTopic.pipeInput(1L, profilering)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue()
                this@with.topologyTestDriver.advanceWallClockTime(1.dager)
                bekreftelseTopic.pipeInput(1L, bekreftelse)
                arenaTopic.isEmpty shouldBe true
                periodeTopic.pipeInput(1L, periodeStopp)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue() should { tilstand ->
                    tilstand.bekreftelse.shouldNotBeNull()
                    tilstand.bekreftelse.periodeId shouldBe periodeStart.id
                    tilstand.bekreftelse.gjelderTil shouldBe bekreftelse.svar.gjelderTil
                    tilstand.bekreftelse.gjelderFra shouldBe bekreftelse.svar.gjelderFra
                    tilstand.bekreftelse.vilFortsetteSomArbeidssoeker shouldBe bekreftelse.svar.vilFortsetteSomArbeidssoeker
                    tilstand.periode.shouldNotBeNull()
                }
            }
            "Når periode avsluttes etter svar 'ja' skal bekreftelsen ikke inkluderes i arena-tilstanden" {
                val bekreftelse = bekreftelseMelding(
                    periodeId = periodeStart.id,
                    tidspunkt = bekreftelseTidspunkt + ettSekund,
                    vilFortsetteSomArbeidssoeker = true
                )
                bekreftelseStore.get(bekreftelse.periodeId) shouldBe null
                periodeTopic.pipeInput(1L, periodeStart)
                this@with.topologyTestDriver.advanceWallClockTime(ettSekund)
                arenaTopic.isEmpty shouldBe true
                profileringsTopic.pipeInput(1L, profilering)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue()
                this@with.topologyTestDriver.advanceWallClockTime(1.dager)
                bekreftelseTopic.pipeInput(1L, bekreftelse)
                arenaTopic.isEmpty shouldBe true
                periodeTopic.pipeInput(1L, periodeStopp)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue() should { tilstand ->
                    tilstand.bekreftelse.shouldBeNull()
                }
            }
        }
    }
})

