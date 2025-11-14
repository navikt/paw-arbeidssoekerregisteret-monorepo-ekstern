package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class BekreftelseMedMetadata(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("status")
    val status: BekreftelsStatus? = null,

    @get:JsonProperty("bekreftelse")
    val bekreftelse: Bekreftelse? = null
) : Hendelse {

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.BEKREFTELSE_V1
}
