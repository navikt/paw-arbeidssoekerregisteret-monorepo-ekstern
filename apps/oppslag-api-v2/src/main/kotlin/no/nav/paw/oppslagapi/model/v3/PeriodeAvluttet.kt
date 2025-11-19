package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant

@JsonTypeName("PERIODE_AVSLUTTET_V1")
data class PeriodeAvluttet(

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PERIODE_AVSLUTTET_V1,

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
) : Hendelse
