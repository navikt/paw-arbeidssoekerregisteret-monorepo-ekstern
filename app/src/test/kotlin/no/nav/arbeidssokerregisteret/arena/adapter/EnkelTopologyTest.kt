package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.*
import java.util.concurrent.atomic.AtomicLong

class EnkelTopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Verifiser standard flyt" - {
            val periode = keySequence.incrementAndGet() to periode(identietsnummer = "12345678901")
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foerstInnsendteOpplysninger = periode.key to opplysninger(periode = periode.melding.id)
            "Når opplysningene blir tilgjengelig sammen med perioden skal vi få noe ut på arena topic" {
                opplysningerTopic.pipeInput(foerstInnsendteOpplysninger.key, foerstInnsendteOpplysninger.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val oppdaterteOpplysninger = foerstInnsendteOpplysninger.key to opplysninger(periode = foerstInnsendteOpplysninger.melding.periodeId)
            "Når opplysningene blir oppdatert skal vi ikke få ut noe på arena topic" {
                opplysningerTopic.pipeInput(oppdaterteOpplysninger.key, oppdaterteOpplysninger.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val profileringAvFoetsteOpplysninger = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = foerstInnsendteOpplysninger.melding.id,
                periode = foerstInnsendteOpplysninger.melding.periodeId
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi få periode, første opplysninger og profilering ut på arena topic" {
                        profileringsTopic.pipeInput(profileringAvFoetsteOpplysninger.key, profileringAvFoetsteOpplysninger.melding)
                        arenaTopic.isEmpty shouldBe false
                        val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                        key shouldBe periode.key
                        assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                        assertApiOpplysningerMatchesArenaOpplysninger(
                            foerstInnsendteOpplysninger.melding,
                            arenaTilstand.opplysningerOmArbeidssoeker
                        )
                        assertApiProfileringMatchesArenaProfilering(profileringAvFoetsteOpplysninger.melding, arenaTilstand.profilering)
                    }
            val profileringForOppdaterteOpplysninger = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = oppdaterteOpplysninger.melding.id,
                periode = oppdaterteOpplysninger.melding.periodeId
            )
            "Når profileringen for oppdaterte opplysninger ankommer og periode og opplysninger er tilgjengelig vi ikke få ut noe" {
                profileringsTopic.pipeInput(profileringForOppdaterteOpplysninger.key, profileringForOppdaterteOpplysninger.melding)
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
                    api = foerstInnsendteOpplysninger.melding,
                    arena = arenaTilstand.opplysningerOmArbeidssoeker
                )
                assertApiProfileringMatchesArenaProfilering(
                    api = profileringAvFoetsteOpplysninger.melding,
                    arena = arenaTilstand.profilering
                )
            }
        }
    }
})

