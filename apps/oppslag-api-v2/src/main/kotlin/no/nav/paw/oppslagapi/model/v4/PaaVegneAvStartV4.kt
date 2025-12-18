package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v3.HendelseType
import java.util.*

@JsonTypeName("PAA_VEGNE_AV_START_V1")
data class PaaVegneAvStartV4(

    @field:JsonProperty("periodeId")
    val periodeId: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("intervalMS")
    val intervalMS: Long,

    @field:JsonProperty("graceMS")
    val graceMS: Long
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PAA_VEGNE_AV_START_V1
}