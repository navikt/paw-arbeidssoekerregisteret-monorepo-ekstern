package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.brukerprofil.kanTilbysTjenesten
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.domain.ProfileringResultat.ANTATT_BEHOV_FOR_VEILEDNING
import no.naw.paw.minestillinger.brukerprofil.flagg.ingenFlagg
import java.time.Instant
import java.util.UUID

data class TestCase(
    val beskrivelse: String,
    val brukerProfil: BrukerProfil,
    val profilering: Profilering,
    val erAdressebeskyttet: (Identitetsnummer) -> Boolean,
    val forventetResultat: Boolean,
)

val arbeidssoekerperiodeId = PeriodeId(UUID.randomUUID())
val brukerProfil = BrukerProfil(
    id = BrukerId(1L),
    identitetsnummer = Identitetsnummer("12345678901"),
    arbeidssoekerperiodeId = arbeidssoekerperiodeId,
    arbeidssoekerperiodeAvsluttet = null,
    listeMedFlagg = ingenFlagg()
)
val profilering = Profilering(
    id = 1L,
    periodeId = arbeidssoekerperiodeId,
    profileringId = UUID.randomUUID(),
    profileringTidspunkt = Instant.now(),
    profileringResultat = ProfileringResultat.ANTATT_GODE_MULIGHETER,
)
val testCases = listOf(
    TestCase(
        beskrivelse = "Er ikke med i testgruppe. Skal returnere false",
        brukerProfil = brukerProfil.medFlagg(
            ErITestGruppenFlagg(false, Instant.now()),
            HarGodeMuligheterFlagg(true, Instant.now()),
            HarGradertAdresseFlagg(false, Instant.now())
        ),
        profilering = profilering,
        erAdressebeskyttet = { false },
        forventetResultat = false,
    ),
    TestCase(
        beskrivelse = "Alt er fint og flott. Skal returnere true",
        brukerProfil = brukerProfil.medFlagg(
            HarGodeMuligheterFlagg(true, Instant.now()),
            HarGradertAdresseFlagg(false, Instant.now()),
            ErITestGruppenFlagg(true, Instant.now())
        ),
        profilering = profilering,
        erAdressebeskyttet = { false },
        forventetResultat = true,
    ),
    TestCase(
        beskrivelse = "Alt er flott, men har gradert adresse. Skal returnere false",
        brukerProfil = brukerProfil.medFlagg(
            HarBruktTjenestenFlagg(true, Instant.now()),
            HarGodeMuligheterFlagg(true, Instant.now()),
            HarGradertAdresseFlagg(true, Instant.now()),
            ErITestGruppenFlagg(true, Instant.now())
        ),
        profilering = profilering,
        erAdressebeskyttet = { true },
        forventetResultat = false,
    ),
    TestCase(
        beskrivelse = "Har brukt tjenesten før har presends over profileringsresultat. Skal returnere true",
        brukerProfil = brukerProfil.medFlagg(
            HarBruktTjenestenFlagg(true, Instant.now()),
            HarGodeMuligheterFlagg(false, Instant.now()),
            ErITestGruppenFlagg(true, Instant.now()),
        ),
        profilering = profilering.copy(profileringResultat = ANTATT_BEHOV_FOR_VEILEDNING),
        erAdressebeskyttet = { false },
        forventetResultat = true,
    )
)

class KanTilbysTjenestenTest : FreeSpec({
    testCases.forEach { testCase ->
        testCase.beskrivelse {
            val resultat = kanTilbysTjenesten(testCase.brukerProfil)
            resultat shouldBe testCase.forventetResultat
        }
    }
})


fun BrukerProfil.medFlagg(vararg flagg: Flagg): BrukerProfil = this.copy(
    listeMedFlagg = this.listeMedFlagg.addOrUpdate(*flagg)
)