package no.naw.paw.brukerprofiler

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.Profilering
import no.naw.paw.brukerprofiler.domain.ProfileringResultat
import no.naw.paw.brukerprofiler.domain.ProfileringResultat.ANTATT_BEHOV_FOR_VEILEDNING
import java.time.Instant
import java.util.UUID

data class TestCase(
    val beskrivelse: String,
    val brukerProfil: BrukerProfil,
    val profilering: Profilering,
    val erAdressebeskyttet: (Identitetsnummer) -> Boolean,
    val forventetResultat: Boolean,
)

val arbeidssoekerperiodeId = UUID.randomUUID()
val brukerProfil = BrukerProfil(
    id = 1L, identitetsnummer = Identitetsnummer("12345678901"),
    tjenestenErAktiv = true,
    kanTilbysTjenesten = KanTilbysTjenesten.JA,
    kanTilbysTjenestenTimestamp = Instant.now(),
    harBruktTjenesten = true,
    arbeidssoekerperiodeId = arbeidssoekerperiodeId,
    erIkkeInteressert = false,
    arbeidssoekerperiodeAvsluttet = null
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
        brukerProfil = brukerProfil.copy(identitetsnummer = Identitetsnummer("23245678901")),
        profilering = profilering,
        erAdressebeskyttet = { false },
        forventetResultat = false,
    ),
    TestCase(
        beskrivelse = "Alt er fint og flott. Skal returnere true",
        brukerProfil = brukerProfil,
        profilering = profilering,
        erAdressebeskyttet = { false },
        forventetResultat = true,
    ),
    TestCase(
        beskrivelse = "Alt er flott, men har gradert adresse. Skal returnere false",
        brukerProfil = brukerProfil,
        profilering = profilering,
        erAdressebeskyttet = { true },
        forventetResultat = false,
    ),
    TestCase(
        beskrivelse = "Har brukt tjenesten før har presends over profileringsresultat. Skal returnere true",
        brukerProfil = brukerProfil.copy(harBruktTjenesten = true),
        profilering = profilering.copy(profileringResultat = ANTATT_BEHOV_FOR_VEILEDNING),
        erAdressebeskyttet = { false },
        forventetResultat = true,
    ),
    TestCase(
        beskrivelse = "Har brukt tjenesten før har presends over profileringsresultat. Skal returnere false",
        brukerProfil = brukerProfil.copy(harBruktTjenesten = false),
        profilering = profilering.copy(profileringResultat = ANTATT_BEHOV_FOR_VEILEDNING),
        erAdressebeskyttet = { false },
        forventetResultat = false,
    )
)

class KanTilbysTjenestenTest : FreeSpec({
    testCases.forEach { testCase ->
        testCase.beskrivelse {
            val resultat = kanTilbysTjenesten(
                brukerProfil = testCase.brukerProfil,
                profilering = testCase.profilering,
                harBeskyttetAdresse = testCase.erAdressebeskyttet,
            )
            resultat shouldBe testCase.forventetResultat
        }
    }
})