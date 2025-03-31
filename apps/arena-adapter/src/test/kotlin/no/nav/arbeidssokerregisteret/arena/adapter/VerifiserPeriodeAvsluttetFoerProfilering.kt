package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.key
import no.nav.arbeidssokerregisteret.arena.adapter.utils.melding
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.opplysninger
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class VerifiserPeriodeAvsluttetFoerProfilering : FreeSpec({
    "Verifiser håndtering av periode avsluttet før profilering er utført" - {
        with(testScope()) {
            val keySequence = AtomicLong(0)
            val periode = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678901",
                startet = metadata(Instant.parse("2024-01-02T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readValue() should { melding ->
                    melding.periode.shouldNotBeNull()
                    melding.profilering.shouldBeNull()
                    melding.opplysningerOmArbeidssoeker.shouldBeNull()
                }
            }
            "Når perioden avsluttes får vi avsluttet melding" {
                val avsluttetPeriode = periode.key to periode(
                    id = periode.melding.id,
                    identietsnummer = periode.melding.identitetsnummer,
                    startet = periode.melding.startet,
                    avsluttet = metadata(
                        periode.melding.startet.tidspunkt + java.time.Duration.ofDays(30)
                    )
                )
                periodeTopic.pipeInput(avsluttetPeriode.key, avsluttetPeriode.melding)
                val (key, value) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe avsluttetPeriode.key
                value should { avsluttet ->
                    avsluttet.shouldNotBeNull()
                    avsluttet.periode.avsluttet.shouldNotBeNull()
                    avsluttet.periode.avsluttet.tidspunkt.shouldNotBeNull()
                    avsluttet.periode.avsluttet.tidspunkt shouldBe avsluttetPeriode.melding.avsluttet.tidspunkt
                    avsluttet.opplysningerOmArbeidssoeker.shouldBeNull()
                    avsluttet.profilering.shouldBeNull()
                }
            }
        }
    }
})

