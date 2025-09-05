package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.OPPGITT_HINDRINGER
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.UDEFINERT
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.UKJENT_VERDI
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import java.time.Instant
import java.util.*

private val timestamp: Instant = Instant.parse("2025-03-15T10:00:59Z")

class DialogmeldingTest : FreeSpec({

    val forventetFooter = DialogTekst.footer(timestamp)

    "Nav: ANTATT_GODE_MULIGHETER, bruker: ANTATT_GODE_MULIGHETER" {
        val dialogmelding = egenvurdering(
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER
        ).tilDialogmelding()

        dialogmelding.overskrift shouldBe DialogTekst.OVERSKRIFT
        dialogmelding.venterPaaSvarFraNav.shouldBeFalse()

        dialogmelding.tekst shouldContain DialogTekst.NAV_GODE_MULIGHETER
        dialogmelding.tekst shouldContain DialogTekst.BRUKER_TRENGER_IKKE_VEILEDNING
        dialogmelding.tekst shouldContain forventetFooter
    }
    "Nav: ANTATT_GODE_MULIGHETER, bruker: ANTATT_BEHOV_FOR_VEILEDNING" {
        val dialogmelding = egenvurdering(
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING
        ).tilDialogmelding()

        dialogmelding.overskrift shouldBe DialogTekst.OVERSKRIFT
        dialogmelding.venterPaaSvarFraNav.shouldBeTrue()

        dialogmelding.tekst shouldContain DialogTekst.NAV_GODE_MULIGHETER
        dialogmelding.tekst shouldContain DialogTekst.BRUKER_TRENGER_VEILEDNING
        dialogmelding.tekst shouldContain forventetFooter
    }


    "Nav: ANTATT_BEHOV_FOR_VEILEDNING, bruker: ANTATT_BEHOV_FOR_VEILEDNING" {
        val dialogmelding = egenvurdering(
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING
        ).tilDialogmelding()

        dialogmelding.overskrift shouldBe DialogTekst.OVERSKRIFT
        dialogmelding.venterPaaSvarFraNav.shouldBeTrue()

        dialogmelding.tekst shouldContain DialogTekst.NAV_BEHOV_FOR_VEILEDNING
        dialogmelding.tekst shouldContain DialogTekst.BRUKER_ØNSKER_HJELP
        dialogmelding.tekst shouldContain forventetFooter
    }

    "Nav: ANTATT_BEHOV_FOR_VEILEDNING, bruker: ANTATT_GODE_MULIGHETER" {
        val dialogmelding = egenvurdering(
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER
        ).tilDialogmelding()

        dialogmelding.overskrift shouldBe DialogTekst.OVERSKRIFT
        dialogmelding.venterPaaSvarFraNav.shouldBeFalse()

        dialogmelding.tekst shouldContain DialogTekst.NAV_BEHOV_FOR_VEILEDNING
        dialogmelding.tekst shouldContain DialogTekst.BRUKER_VIL_KLARE_SEG_SELV
        dialogmelding.tekst shouldContain forventetFooter
    }


    "Ustøttede kombinasjoner/profileringer" {
        shouldThrow<ProfileringKombinasjonIkkeStøttet> {
            egenvurdering(ANTATT_GODE_MULIGHETER, OPPGITT_HINDRINGER).tilDialogmelding()
        }
        shouldThrow<ProfileringKombinasjonIkkeStøttet> {
            egenvurdering(ANTATT_GODE_MULIGHETER, UDEFINERT).tilDialogmelding()
        }
        shouldThrow<ProfileringKombinasjonIkkeStøttet> {
            egenvurdering(ANTATT_BEHOV_FOR_VEILEDNING, UKJENT_VERDI).tilDialogmelding()
        }

        shouldThrow<ProfileringIkkeStøttet> {
            egenvurdering(OPPGITT_HINDRINGER, ANTATT_GODE_MULIGHETER).tilDialogmelding()
        }
        shouldThrow<ProfileringIkkeStøttet> {
            egenvurdering(UDEFINERT, ANTATT_GODE_MULIGHETER).tilDialogmelding()
        }
        shouldThrow<ProfileringIkkeStøttet> {
            egenvurdering(UKJENT_VERDI, ANTATT_GODE_MULIGHETER).tilDialogmelding()
        }
    }

})

fun egenvurdering(
    navProfilering: ProfilertTil,
    brukersEgenvurdering: ProfilertTil,
    tidspunkt: Instant = timestamp,
) = Egenvurdering(
    UUID.randomUUID(),
    UUID.randomUUID(),
    UUID.randomUUID(),
    UUID.randomUUID(),
    Metadata().apply<Metadata> { setTidspunkt(tidspunkt) },
    navProfilering,
    brukersEgenvurdering
)
