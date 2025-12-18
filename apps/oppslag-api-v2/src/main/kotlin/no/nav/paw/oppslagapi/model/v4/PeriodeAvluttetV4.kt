package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.Bruker
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.TidspunktFraKilde
import java.time.Instant

@JsonTypeName("PERIODE_AVSLUTTET_V1")
data class PeriodeAvluttetV4(

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
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PERIODE_AVSLUTTET_V1
}
