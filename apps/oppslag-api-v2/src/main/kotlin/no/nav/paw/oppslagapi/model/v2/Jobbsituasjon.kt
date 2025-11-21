package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class Jobbsituasjon(

    @get:JsonProperty("beskrivelser")
    val beskrivelser: List<BeskrivelseMedDetaljer>
)