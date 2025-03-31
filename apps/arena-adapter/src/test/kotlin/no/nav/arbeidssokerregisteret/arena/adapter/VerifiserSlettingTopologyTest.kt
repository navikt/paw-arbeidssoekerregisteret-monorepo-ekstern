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
import java.time.Duration.ofDays
import java.time.Instant.now
import java.util.concurrent.atomic.AtomicLong

class VerifiserSlettingTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Når vi har sendt, periode start og stopp, opplysninger" - {
            val periode = keySequence.incrementAndGet() to periode(identietsnummer = "12345678901")
            "Når bare perioden er sendt inn skal vi få den ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe false
                arenaTopic.readKeyValue() should {
                    it.value.profilering.shouldBeNull()
                    it.value.opplysningerOmArbeidssoeker.shouldBeNull()
                    it.value.periode.shouldNotBeNull()
                    it.key shouldBe periode.key

                }
            }
            "stream time settes flere dager frem i tid " {
                val ubruktePeriode = keySequence.incrementAndGet() to periode(
                    identietsnummer = "12345678902",
                    startet = metadata(now() + ofDays(30))
                )
                periodeTopic.pipeInput(
                    ubruktePeriode.key,
                    ubruktePeriode.melding,
                    ubruktePeriode.melding.startet.tidspunkt
                )
                arenaTopic.readValue()
            }
            "Join store skal inneholde periode" {
                val topicsJoin = joinStore.get(periode.melding.id)
                topicsJoin.shouldNotBeNull()
            }
            "Nå perioden avsluttes får vi melding på topic" {
                val avsluttet = periode(
                    id = periode.melding.id,
                    identietsnummer = periode.melding.identitetsnummer,
                    startet = periode.melding.startet,
                    avsluttet = metadata(
                        periode.melding.startet.tidspunkt + ofDays(30)
                    )
                )
                periodeTopic.pipeInput(periode.key, avsluttet)
                arenaTopic.isEmpty shouldBe false
                val keyValue = arenaTopic.readKeyValue()
                keyValue.key shouldBe periode.key
                keyValue.value.periode?.avsluttet?.tidspunkt shouldBe avsluttet.avsluttet.tidspunkt
            }
            "join store skal ikke lenger inneholde topics joind for periode: ${periode.melding.id}" {
                topologyTestDriver.advanceWallClockTime(ofDays(0))
                joinStore.get(periode.melding.id) shouldBe null
            }
        }
    }
})

