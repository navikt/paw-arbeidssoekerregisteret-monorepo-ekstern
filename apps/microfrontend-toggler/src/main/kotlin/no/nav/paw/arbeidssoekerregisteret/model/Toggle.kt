package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

enum class ToggleSource(@get:JsonValue val value: String) {
    ARBEIDSSOEKERPERIODE("arbeidsøkerperiode"),
    SISTE_14A_VEDTAK("siste-14a-vedtak")
}

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

fun ToggleRequest.buildToggle(identitetsnummer: Identitetsnummer): Toggle {
    val sensitivitet = if (action == ToggleAction.ENABLE) Sensitivitet.HIGH else null

    return Toggle(
        action = action,
        ident = identitetsnummer.verdi,
        microfrontendId = microfrontendId,
        sensitivitet = sensitivitet,
        initialedBy = "paw" // TODO Styre dette med konfig/miljøvar?
    )
}
