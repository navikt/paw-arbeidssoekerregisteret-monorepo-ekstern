package no.naw.paw.minestillinger.brukerprofil

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.AdressebeskyttelseMåSjekkes
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.Ja
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.Nei
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.PeriodeId
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TjenestenKanAktiveresTest : FreeSpec({

    "Test av tjenestenKanAktiveres funksjonen" {
        val clock = object: Clock {
            override fun now(): Instant {
                return Instant.now()
            }
        }
        listOf(
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(false, clock.now()),
                    HarBeskyttetadresseFlagg(false, clock.now()),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(true, clock.now()),
                    HarGodeMuligheterFlagg(true, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = Ja
            ),
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(false, clock.now()),
                    HarBeskyttetadresseFlagg(true, clock.now()),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(true, clock.now()),
                    HarGodeMuligheterFlagg(true, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = Nei
            ),
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(false, clock.now()),
                    HarBeskyttetadresseFlagg(false, clock.now() - Duration.ofDays(2)),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(true, clock.now()),
                    HarGodeMuligheterFlagg(true, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = AdressebeskyttelseMåSjekkes
            ),
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(true, clock.now()),
                    HarBeskyttetadresseFlagg(false, clock.now()),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(true, clock.now()),
                    HarGodeMuligheterFlagg(false, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = TjenestenKanAktiveresResultat.Ja
            ),
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(false, clock.now()),
                    HarBeskyttetadresseFlagg(false, clock.now() - Duration.ofDays(10)),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(true, clock.now()),
                    HarGodeMuligheterFlagg(false, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = Nei
            ),
            TjenstenKanAktiveresTestCase(
                gjeldeneFlaggtype = listOf(
                    HarBruktTjenestenFlagg(false, clock.now()),
                    HarBeskyttetadresseFlagg(false, clock.now() - Duration.ofDays(10)),
                    TjenestenErAktivFlagg(false, clock.now()),
                    ErITestGruppenFlagg(false, clock.now()),
                    HarGodeMuligheterFlagg(true, clock.now())
                ),
                tidspunkt = clock.now(),
                graderingGyldighet = Duration.ofDays(1),
                forventetResultat = Nei
            )
        ).forEach { testCase ->
            val brukerProfil = BrukerProfil(
                id = BrukerId(1),
                identitetsnummer = Identitetsnummer("12345678901"),
                listeMedFlagg = ListeMedFlagg.listeMedFlagg(testCase.gjeldeneFlaggtype),
                arbeidssoekerperiodeId = PeriodeId(UUID.randomUUID()),
                arbeidssoekerperiodeAvsluttet = null
            )
            val resultat = brukerProfil.tjenestenKanAktiveres(
                tidspunkt = testCase.tidspunkt,
                adressebeskyttelseGyldighet = testCase.graderingGyldighet
            )
            resultat shouldBe testCase.forventetResultat
        }
    }
})

data class TjenstenKanAktiveresTestCase(
    val gjeldeneFlaggtype: Iterable<Flagg>,
    val tidspunkt: Instant,
    val graderingGyldighet: Duration,
    val forventetResultat: TjenestenKanAktiveresResultat
)
