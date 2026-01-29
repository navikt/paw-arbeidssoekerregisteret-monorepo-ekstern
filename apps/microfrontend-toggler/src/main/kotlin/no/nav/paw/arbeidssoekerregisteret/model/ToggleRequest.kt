package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.paw.felles.model.Identitetsnummer

data class ToggleRequest(
    @field:JsonProperty("@action") val action: ToggleAction,
    @field:JsonProperty("microfrontend_id") val microfrontendId: String
) {
    fun asToggle(
        identitetsnummer: Identitetsnummer,
        defaultSensitivitet: Sensitivitet,
        initiatedBy: String = "paw" // TODO Styre dette med konfig/miljøvar?
    ): Toggle {
        val sensitivitet = if (action == ToggleAction.ENABLE) defaultSensitivitet else null

        return Toggle(
            action = action,
            ident = identitetsnummer.value,
            microfrontendId = microfrontendId,
            sensitivitet = sensitivitet,
            initiatedBy = initiatedBy
        )
    }
}