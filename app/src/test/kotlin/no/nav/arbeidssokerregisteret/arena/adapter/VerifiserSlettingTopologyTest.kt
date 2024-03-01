package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.*
import no.nav.paw.arbeidssokerregisteret.arena.adapter.compoundKey
import no.nav.paw.arbeidssokerregisteret.arena.adapter.topology
import java.time.Duration.ofDays
import java.time.Instant
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
            "Når opplysningene blir tilgjengelig sammen med perioden skal vi få noe ut på arena topic" {
                opplysningerTopic.pipeInput(opplysninger.key, opplysninger.melding )
                arenaTopic.isEmpty shouldBe true
            }
            "Join store skal inneholde periode og opplysninger" {
                val topicsJoin = joinStore.get(compoundKey(periode.key, periode.melding.id))
                topicsJoin.shouldNotBeNull()
            }
            "stream time settes flere dager frem i tid " {
                val ubruktePeriode = keySequence.incrementAndGet() to periode(
                    identietsnummer = "12345678902",
                    startet = metadata(now() + ofDays(30))
                )
                periodeTopic.pipeInput(ubruktePeriode.key, ubruktePeriode.melding, ubruktePeriode.melding.startet.tidspunkt)
            }
            "join store skal ikke lenger inneholde topics joind for periode: ${periode.melding.id}" {
                joinStore.get(compoundKey(periode.key, periode.melding.id)) shouldBe null
            }
        }
    }
})

