package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Frist(

    @get:JsonProperty("type")
    val type: FristType,

    @get:JsonProperty("verdi")
    val verdi: kotlin.String? = null,

    @get:JsonProperty("dato")
    val dato: LocalDate? = null
)