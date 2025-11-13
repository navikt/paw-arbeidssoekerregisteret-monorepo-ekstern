package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class PaaVegneAvStart(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("intervalMS")
    val intervalMS: Long,

    @get:JsonProperty("graceMS")
    val graceMS: Long
) : Hendelse {

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.PA_VEGNE_AV_START_V1
}