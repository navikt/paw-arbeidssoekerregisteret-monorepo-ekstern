package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class VerifiserHåndteringAvManglendeProfilering : FreeSpec({
    "Verifiser håndtering av manglende profilering" - {
        with(testScope()) {
            val keySequence = AtomicLong(0)
            val periode = keySequence.incrementAndGet() to periode(
                identietsnummer = "12345678902",
                startet = metadata(Instant.parse("2024-01-03T00:00:00Z"))
            )
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val foerstInnsendteOpplysninger = periode.key to opplysninger(
                periode = periode.melding.id,
                timestamp = Instant.parse("2024-01-03T00:00:01Z"),
                kilde = "veilarbregistrering"
            )
            "Når nye opplysningene blir tilgjengelig skal vi ikke få noe ut på arena topic" {
                opplysningerTopic.pipeInput(
                    foerstInnsendteOpplysninger.key,
                    foerstInnsendteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe true
            }
            val profileringAvFoertsteOpplysninger = foerstInnsendteOpplysninger.key to profilering(
                opplysningerId = foerstInnsendteOpplysninger.melding.id,
                periode = foerstInnsendteOpplysninger.melding.periodeId,
                timestamp = Instant.parse("2024-01-02T00:00:03Z")
            )
            "Når profileringen ankommer og periode og opplysninger fra veilarb er tilgjengelig skal ingenting skje" {
                profileringsTopic.pipeInput(
                    profileringAvFoertsteOpplysninger.key,
                    profileringAvFoertsteOpplysninger.melding
                )
                arenaTopic.isEmpty shouldBe true
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

