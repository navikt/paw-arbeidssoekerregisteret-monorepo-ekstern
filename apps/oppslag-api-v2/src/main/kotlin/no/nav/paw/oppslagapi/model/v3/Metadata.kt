package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Metadata(

    @get:JsonProperty("type")
    override val type: HendelseType,

    @get:JsonProperty("tidspunkt")
    val tidspunkt: Instant,

    @get:JsonProperty("utfoertAv")
    val utfoertAv: Bruker,

    @get:JsonProperty("kilde")
    val kilde: String,

    @get:JsonProperty("aarsak")
    val aarsak: String,

    @get:JsonProperty("tidspunktFraKilde")
    val tidspunktFraKilde: TidspunktFraKilde? = null
) : Hendelse