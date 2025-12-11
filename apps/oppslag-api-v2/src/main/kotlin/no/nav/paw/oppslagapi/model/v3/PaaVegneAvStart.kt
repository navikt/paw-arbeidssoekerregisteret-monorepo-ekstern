package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import java.util.*

@JsonTypeName("PAA_VEGNE_AV_START_V1")
data class PaaVegneAvStart(

    @field:JsonProperty("periodeId")
    val periodeId: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("intervalMS")
    val intervalMS: Long,

    @field:JsonProperty("graceMS")
    val graceMS: Long,

    @get:JsonProperty("tidspunkt")
    override val tidspunkt: Instant
) : Hendelse {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PAA_VEGNE_AV_START_V1
}