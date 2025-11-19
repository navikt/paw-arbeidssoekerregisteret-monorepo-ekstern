package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class BeskrivelseMedDetaljer(

    @field:JsonProperty("beskrivelse")
    val beskrivelse: Beskrivelse = Beskrivelse.UKJENT_VERDI,

    @field:JsonProperty("detaljer")
    val detaljer: Map<String, String>
)