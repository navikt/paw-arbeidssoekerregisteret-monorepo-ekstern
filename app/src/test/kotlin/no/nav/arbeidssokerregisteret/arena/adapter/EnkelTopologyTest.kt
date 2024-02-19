package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.*
import java.util.concurrent.atomic.AtomicLong

class EnkelTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Når vi har sendt, periode, opplysninger og profilering skal vi få noe ut på arena topic" - {
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
            val profilering = opplysninger.key to profilering(
                opplysningerId = opplysninger.melding.id,
                periode = opplysninger.melding.periodeId
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi få noe ut på arena topic" {
                profileringsTopic.pipeInput(profilering.key, profilering.melding)
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe periode.key
                assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                assertApiOpplysningerMatchesArenaOpplysninger(opplysninger.melding, arenaTilstand.opplysningerOmArbeidssoeker)
                assertApiProfileringMatchesArenaProfilering(profilering.melding, arenaTilstand.profilering)
            }
        }
    }
})

