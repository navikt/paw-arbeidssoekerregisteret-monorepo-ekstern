package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class Tidslinje(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @get:JsonProperty("startet")
    val startet: Instant,

    @get:JsonProperty("avsluttet")
    val avsluttet: Instant? = null,

    @get:JsonProperty("hendelser")
    val hendelser: List<Hendelse>
)
