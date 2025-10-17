package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import java.time.Instant
import java.util.UUID

class BrukerprofilTjenesteTest : FreeSpec({
    val brukerprofil = BrukerProfil(
        id = 1,
        identitetsnummer = Identitetsnummer("12345678901"),
        kanTilbysTjenesten = KanTilbysTjenesten.JA,
        kanTilbysTjenestenTimestamp = Instant.now(),
        arbeidssoekerperiodeId = UUID.randomUUID(),
        harBruktTjenesten = false,
        tjenestenErAktiv = false,
        erIkkeInteressert = false,
        arbeidssoekerperiodeAvsluttet = null
    )
    val profilering = Profilering(
        id = 1,
        periodeId = brukerprofil.arbeidssoekerperiodeId,
        profileringId = UUID.randomUUID(),
        profileringTidspunkt = Instant.now(),
        profileringResultat = ProfileringResultat.ANTATT_BEHOV_FOR_VEILEDNING
    )
    "Når det er mindre enn 24 timer siden kanTilbysTjenesten ble satt" - {
        "og kanTilbysTjenesten er JA, så skal kanTilbysTjenestenErUtdatert returnere true" {

        }
        "og kanTilbysTjenesten er NEI, så skal kanTilbysTjenestenErUtdatert returnere false" {
        }
    }

})
