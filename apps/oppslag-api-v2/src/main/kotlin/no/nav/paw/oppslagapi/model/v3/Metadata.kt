package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Metadata(

    @field:JsonProperty("tidspunkt")
    val tidspunkt: Instant,

    @field:JsonProperty("utfoertAv")
    val utfoertAv: Bruker,

    @field:JsonProperty("kilde")
    val kilde: String,

    @field:JsonProperty("aarsak")
    val aarsak: String,

    @field:JsonProperty("tidspunktFraKilde")
    val tidspunktFraKilde: TidspunktFraKilde? = null
)