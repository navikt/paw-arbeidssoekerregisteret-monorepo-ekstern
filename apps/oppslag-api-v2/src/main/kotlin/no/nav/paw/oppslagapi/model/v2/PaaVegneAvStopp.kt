package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class PaaVegneAvStopp(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("fristBrutt")
    val fristBrutt: Boolean? = false
)