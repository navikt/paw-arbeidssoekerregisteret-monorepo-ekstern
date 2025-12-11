package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import java.util.*

@JsonTypeName("PAA_VEGNE_AV_STOPP_V1")
data class PaaVegneAvStopp(

    @field:JsonProperty("periodeId")
    val periodeId: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("fristBrutt")
    val fristBrutt: Boolean? = false,

    @get:JsonProperty("tidspunkt")
    override val tidspunkt: Instant
) : Hendelse {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PAA_VEGNE_AV_STOPP_V1
}