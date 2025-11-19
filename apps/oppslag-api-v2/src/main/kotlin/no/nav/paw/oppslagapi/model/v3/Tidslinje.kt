package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class Tidslinje(

    @field:JsonProperty("periodeId")
    val periodeId: UUID,

    @field:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @field:JsonProperty("startet")
    val startet: Instant,

    @field:JsonProperty("avsluttet")
    val avsluttet: Instant? = null,

    @field:JsonProperty("hendelser")
    val hendelser: List<Hendelse>
)
