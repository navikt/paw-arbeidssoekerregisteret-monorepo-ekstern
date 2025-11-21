package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Periode(

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @get:JsonProperty("startet")
    val startet: Metadata,

    @get:JsonProperty("avsluttet")
    val avsluttet: Metadata? = null
)