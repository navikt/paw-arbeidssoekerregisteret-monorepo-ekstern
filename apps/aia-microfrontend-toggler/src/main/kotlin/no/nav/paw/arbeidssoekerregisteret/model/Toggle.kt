package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

private const val ENABLE_ACTION = "enable"
private const val DISABLE_ACTION = "disable"
private const val SENSITIVITET_HIGH = "high"
const val AIA_MIN_SIDE = "aia-min-side"
const val AIA_BEHOVSVURDERING = "aia-behovsvurdering"

@JvmRecord
data class Toggle(
    @JsonProperty("@action") val action: String,
    val ident: String,
    @JsonProperty("microfrontend_id") val microfrontendId: String,
    val sensitivitet: String? = null,
    @JsonProperty("@initiated_by") val initialedBy: String
)

@JvmRecord
data class ToggleState(val periode: PeriodeInfo, val toggle: Toggle)

fun buildEnableToggle(identitetsnummer: String, microfrontendId: String): Toggle {
    return Toggle(
        action = ENABLE_ACTION,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        sensitivitet = SENSITIVITET_HIGH,
        initialedBy = "paw" // TODO Bruke miljøvariabel
    )
}

fun buildDisableToggle(identitetsnummer: String, microfrontendId: String): Toggle {
    return Toggle(
        action = DISABLE_ACTION,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        initialedBy = "paw" // TODO Bruke miljøvariabel
    )
}
