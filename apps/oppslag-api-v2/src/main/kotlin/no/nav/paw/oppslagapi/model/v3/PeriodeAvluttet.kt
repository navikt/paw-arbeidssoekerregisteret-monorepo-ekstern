package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant

@JsonTypeName("PERIODE_AVSLUTTET_V1")
data class PeriodeAvluttet(

    @field:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @get:JsonProperty("tidspunkt")
    override val tidspunkt: Instant
) : Hendelse {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PERIODE_AVSLUTTET_V1
}
