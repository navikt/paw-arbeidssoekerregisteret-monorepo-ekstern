package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

enum class ToggleAction(@get:JsonValue val value: String) {
    ENABLE("enable"),
    DISABLE("disable")
}

enum class Sensitivitet(@get:JsonValue val value: String) {
    HIGH("high"),
    SUBSTANTIAL("substantial")
}

data class Toggle(
    @JsonProperty("@action") val action: ToggleAction,
    val ident: String,
    @JsonProperty("microfrontend_id") val microfrontendId: String,
    val sensitivitet: Sensitivitet? = null,
    @JsonProperty("@initiated_by") val initialedBy: String
)

data class ToggleState(val periode: PeriodeInfo, val toggle: Toggle)

data class ToggleRequest(
    @JsonProperty("@action") val action: ToggleAction,
    @JsonProperty("microfrontend_id") val microfrontendId: String
)

fun buildEnableToggle(identitetsnummer: String, microfrontendId: String): Toggle {
    return Toggle(
        action = ToggleAction.ENABLE,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        sensitivitet = Sensitivitet.HIGH,
        initialedBy = "paw" // TODO Bruke miljøvariabel
    )
}

fun buildDisableToggle(identitetsnummer: String, microfrontendId: String): Toggle {
    return Toggle(
        action = ToggleAction.DISABLE,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        initialedBy = "paw" // TODO Bruke miljøvariabel
    )
}

fun buildDisableToggleState(periode: PeriodeInfo, microfrontendId: String): ToggleState {
    return ToggleState(periode, buildDisableToggle(periode.identitetsnummer, microfrontendId))
}

fun buildDisableToggleRecord(periode: PeriodeInfo, microfrontendId: String): Record<Long, ToggleState> {
    return Record(
        periode.arbeidssoekerId,
        ToggleState(
            periode,
            buildDisableToggle(periode.identitetsnummer, microfrontendId)
        ),
        Instant.now().toEpochMilli()
    )
}

fun buildEnableToggleRecord(periode: PeriodeInfo, microfrontendId: String): Record<Long, ToggleState> {
    return Record(
        periode.arbeidssoekerId,
        ToggleState(
            periode,
            buildEnableToggle(periode.identitetsnummer, microfrontendId)
        ),
        Instant.now().toEpochMilli()
    )
}
