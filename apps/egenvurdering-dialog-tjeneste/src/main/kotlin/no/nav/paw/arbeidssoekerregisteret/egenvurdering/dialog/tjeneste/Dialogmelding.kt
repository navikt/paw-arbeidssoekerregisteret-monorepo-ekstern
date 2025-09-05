package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import java.time.Instant

data class Dialogmelding(
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraNav: Boolean,
)

fun Egenvurdering.tilDialogmelding(): Dialogmelding {
    val egenvurderingMottattTidspunkt = this.sendtInnAv.tidspunkt
    val navProfilering = this.profilertTil
    val brukersEgenvurdering = this.egenvurdering
    return when (navProfilering) {
        ANTATT_GODE_MULIGHETER -> when (brukersEgenvurdering) {
            ANTATT_GODE_MULIGHETER -> lagDialogmelding(
                navVurderingTekst = DialogTekst.NAV_GODE_MULIGHETER,
                brukerVurderingTekst = DialogTekst.BRUKER_TRENGER_IKKE_VEILEDNING,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = false
            )

            ANTATT_BEHOV_FOR_VEILEDNING -> lagDialogmelding(
                navVurderingTekst = DialogTekst.NAV_GODE_MULIGHETER,
                brukerVurderingTekst = DialogTekst.BRUKER_TRENGER_VEILEDNING,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = true
            )

            else -> throw ProfileringKombinasjonIkkeStøttet(navProfilering, brukersEgenvurdering)
        }

        ANTATT_BEHOV_FOR_VEILEDNING -> when (brukersEgenvurdering) {
            ANTATT_BEHOV_FOR_VEILEDNING -> lagDialogmelding(
                navVurderingTekst = DialogTekst.NAV_BEHOV_FOR_VEILEDNING,
                brukerVurderingTekst = DialogTekst.BRUKER_ØNSKER_HJELP,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = true
            )

            ANTATT_GODE_MULIGHETER -> lagDialogmelding(
                navVurderingTekst = DialogTekst.NAV_BEHOV_FOR_VEILEDNING,
                brukerVurderingTekst = DialogTekst.BRUKER_VIL_KLARE_SEG_SELV,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = false
            )

            else -> throw ProfileringKombinasjonIkkeStøttet(navProfilering, brukersEgenvurdering)
        }

        else -> throw ProfileringIkkeStøttet(navProfilering)
    }
}

private fun lagDialogmelding(
    navVurderingTekst: String,
    brukerVurderingTekst: String,
    egenvurderingMottatt: Instant,
    ventPåSvarFraNav: Boolean,
) = Dialogmelding(
    overskrift = DialogTekst.OVERSKRIFT,
    venterPaaSvarFraNav = ventPåSvarFraNav,
    tekst = """
        $navVurderingTekst
    
        $brukerVurderingTekst
    
        ${DialogTekst.footer(egenvurderingMottatt)}"
    """.trimIndent()
)

class ProfileringKombinasjonIkkeStøttet(
    navProfilering: ProfilertTil,
    brukersEgenvurdering: ProfilertTil,
) : UnsupportedOperationException(
    "Kombinasjonen støtter ikke bygging av dialogmelding: Nav profilering=$navProfilering, brukers egenvurdering=$brukersEgenvurdering"
)

class ProfileringIkkeStøttet(
    profilertTil: ProfilertTil,
) : UnsupportedOperationException(
    "Profilering ikke støttet for bygging av dialogmelding: profilertTil=$profilertTil"
)
