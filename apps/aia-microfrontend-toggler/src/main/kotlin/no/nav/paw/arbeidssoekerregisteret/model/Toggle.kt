package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

@JvmRecord
data class Toggle(
    @JsonProperty("@action") val action: String,
    val ident: String,
    @JsonProperty("microfrontend_id") val microfrontendId: String,
    val sensitivitet: String,
    @JsonProperty("@initiated_by") val initialedBy: String
)

@JvmRecord
data class ToggleState(val periode: PeriodeInfo)

@JvmRecord
data class PeriodeInfo(
    val id: UUID,
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val startet: Instant,
    val avsluttet: Instant?
)
