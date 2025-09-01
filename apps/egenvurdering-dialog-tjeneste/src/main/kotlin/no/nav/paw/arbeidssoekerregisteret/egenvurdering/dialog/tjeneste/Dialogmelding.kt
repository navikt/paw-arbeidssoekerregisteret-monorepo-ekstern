package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.formaterDato
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import java.time.Instant

data class Dialogmelding(
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraNav: Boolean,
)

internal const val OVERSKRIFT = "Egenvurdering"
internal const val NAV_GODE_MULIGHETER =
    "Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav."
internal const val NAV_BEHOV_FOR_VEILEDNING =
    "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid."
internal const val BRUKER_TRENGER_IKKE_VEILEDNING = "Min vurdering: Jeg klarer meg uten veileder"
internal const val BRUKER_TRENGER_VEILEDNING = "Min vurdering: Jeg trenger en veileder for å komme i arbeid"
internal const val BRUKER_ØNSKER_HJELP = "Min vurdering: Ja, jeg ønsker hjelp"
internal const val BRUKER_VIL_KLARE_SEG_SELV = "Min vurdering: Nei, jeg vil gjerne klare meg selv"
internal const val FOOTER_TEKST = "Dette er en automatisk generert melding basert på egenvurdering mottatt fra bruker:"

fun Egenvurdering.tilDialogmelding(): Dialogmelding {
    val egenvurderingMottattTidspunkt = this.sendtInnAv.tidspunkt
    val navProfilering = this.profilertTil
    val brukersEgenvurdering = this.egenvurdering
    return when (navProfilering) {
        ANTATT_GODE_MULIGHETER -> when (brukersEgenvurdering) {
            ANTATT_GODE_MULIGHETER -> lagDialogmelding(
                navVurderingTekst = NAV_GODE_MULIGHETER,
                brukerVurderingTekst = BRUKER_TRENGER_IKKE_VEILEDNING,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = false
            )

            ANTATT_BEHOV_FOR_VEILEDNING -> lagDialogmelding(
                navVurderingTekst = NAV_GODE_MULIGHETER,
                brukerVurderingTekst = BRUKER_TRENGER_VEILEDNING,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = true
            )

            else -> error("Ustøttet kombinasjon: Nav vurdering=$navProfilering, brukers egenvurdering=$brukersEgenvurdering")
        }

        ANTATT_BEHOV_FOR_VEILEDNING -> when (brukersEgenvurdering) {
            ANTATT_BEHOV_FOR_VEILEDNING -> lagDialogmelding(
                navVurderingTekst = NAV_BEHOV_FOR_VEILEDNING,
                brukerVurderingTekst = BRUKER_ØNSKER_HJELP,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = true
            )

            ANTATT_GODE_MULIGHETER -> lagDialogmelding(
                navVurderingTekst = NAV_BEHOV_FOR_VEILEDNING,
                brukerVurderingTekst = BRUKER_VIL_KLARE_SEG_SELV,
                egenvurderingMottatt = egenvurderingMottattTidspunkt,
                ventPåSvarFraNav = false
            )

            else -> error("Ustøttet kombinasjon: Nav vurdering=$navProfilering, brukers egenvurdering=$brukersEgenvurdering")
        }

        else -> error("Ustøttet profilertTil: $navProfilering")
    }
}

private fun lagDialogmelding(
    navVurderingTekst: String,
    brukerVurderingTekst: String,
    egenvurderingMottatt: Instant,
    ventPåSvarFraNav: Boolean,
) = Dialogmelding(
    overskrift = OVERSKRIFT,
    venterPaaSvarFraNav = ventPåSvarFraNav,
    tekst = """
        $navVurderingTekst
    
        $brukerVurderingTekst
    
        $FOOTER_TEKST ${formaterDato(egenvurderingMottatt)}."
    """.trimIndent()
)
