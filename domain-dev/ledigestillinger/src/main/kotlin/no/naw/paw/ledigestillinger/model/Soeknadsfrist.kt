package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Soeknadsfrist(
    @get:JsonProperty("raw")
    val raw: kotlin.String? = null,
    @get:JsonProperty("fristType")
    val fristType: SoeknadsfristType? = null,
    @get:JsonProperty("dato")
    val dato: LocalDate? = null
)