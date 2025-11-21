package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Bekreftelse(

    @get:JsonProperty("periodeId")
    val periodeId: UUID,

    @get:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("svar")
    val svar: Svar
)