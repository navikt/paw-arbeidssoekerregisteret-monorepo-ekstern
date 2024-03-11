package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
        "Når vi har sendt, periode, opplysninger" - {
            val periode = keySequence.incrementAndGet() to periode(identietsnummer = "12345678901")
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val opplysninger = periode.key to opplysninger(periode = periode.melding.id)
            "Når opplysningene blir tilgjengelig sammen med perioden skal vi ikke få noe ut på arena topic" {
                opplysningerTopic.pipeInput(opplysninger.key, opplysninger.melding)
                arenaTopic.isEmpty shouldBe true
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
            }
            "Join store skal inneholde periode og opplysninger" {
                val topicsJoin = joinStore.get(periode.melding.id)
                topicsJoin.shouldNotBeNull()
            }
            "Nå perioden avsluttes får vi ikke melding på topic" {
                val avsluttet = periode(
                    id = periode.melding.id,
                    identietsnummer = periode.melding.identitetsnummer,
                    startet = periode.melding.startet,
                    avsluttet = metadata(
                        periode.melding.startet.tidspunkt + ofDays(30)
                    )
                )
                periodeTopic.pipeInput(periode.key, avsluttet)
                arenaTopic.isEmpty shouldBe true
            }
            "stream time settes flere dager frem i tid igjen" {
                val ubruktePeriode = keySequence.incrementAndGet() to periode(
                    identietsnummer = "12345678902",
                    startet = metadata(now() + ofDays(60))
                )
                periodeTopic.pipeInput(
                    ubruktePeriode.key,
                    ubruktePeriode.melding,
                    ubruktePeriode.melding.startet.tidspunkt
                )
            }
            "join store skal ikke lenger inneholde topics joind for periode: ${periode.melding.id}" {
                joinStore.get(periode.melding.id) shouldBe null
            }
        }
    }
})

