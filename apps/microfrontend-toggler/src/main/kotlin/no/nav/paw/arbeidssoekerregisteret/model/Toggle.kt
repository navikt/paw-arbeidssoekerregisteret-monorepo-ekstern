package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

data class Toggle(
    @field:JsonProperty("@action") val action: ToggleAction,
    val ident: String,
    @field:JsonProperty("microfrontend_id") val microfrontendId: String,
    val sensitivitet: Sensitivitet? = null,
    @field:JsonProperty("@initiated_by") val initiatedBy: String
) {
    fun asRecord(arbeidssoekerId: Long): Record<Long, Toggle> {
        return Record(
            arbeidssoekerId,
            this,
            Instant.now().toEpochMilli()
        )
    }
}