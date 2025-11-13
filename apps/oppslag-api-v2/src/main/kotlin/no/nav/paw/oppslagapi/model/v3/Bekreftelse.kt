package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("BEKREFTELSE_V1")
data class Bekreftelse(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("status")
    val status: BekreftelsStatus,

    @field:JsonProperty("svar")
    val svar: Svar
) : Hendelse {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.BEKREFTELSE_V1
}