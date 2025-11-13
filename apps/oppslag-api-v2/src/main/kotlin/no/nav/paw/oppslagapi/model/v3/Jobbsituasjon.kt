package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class Jobbsituasjon(

    @field:JsonProperty("beskrivelser")
    val beskrivelser: List<BeskrivelseMedDetaljer>
)