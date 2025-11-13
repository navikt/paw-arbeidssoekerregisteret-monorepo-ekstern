package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class PaaVegneAvStopp(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("fristBrutt")
    val fristBrutt: Boolean? = false
) : Hendelse {

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.PA_VEGNE_AV_STOPP_V1
}