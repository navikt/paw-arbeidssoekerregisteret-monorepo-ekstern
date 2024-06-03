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

fun Toggle.buildRecord(arbeidssoekerId: Long): Record<Long, Toggle> {
    return Record(
        arbeidssoekerId,
        this,
        Instant.now().toEpochMilli()
    )
}

data class ToggleRequest(
    @JsonProperty("@action") val action: ToggleAction,
    @JsonProperty("microfrontend_id") val microfrontendId: String
)

fun ToggleRequest.buildToggle(identitetsnummer: String): Toggle {
    val sensitivitet = if (action == ToggleAction.ENABLE) Sensitivitet.HIGH else null

    return Toggle(
        action = action,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        sensitivitet = sensitivitet,
        initialedBy = "paw" // TODO Bruke miljøvariabel
    )
}
