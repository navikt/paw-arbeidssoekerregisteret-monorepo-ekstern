package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v3.HendelseType
import java.util.*

@JsonTypeName("PAA_VEGNE_AV_STOPP_V1")
data class PaaVegneAvStoppV4(

    @field:JsonProperty("periodeId")
    val periodeId: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("fristBrutt")
    val fristBrutt: Boolean? = false
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PAA_VEGNE_AV_STOPP_V1
}