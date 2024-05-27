package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

private const val ENABLE_ACTION = "enable"
private const val DISABLE_ACTION = "disable"
private const val SENSITIVITET_HIGH = "high"

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
