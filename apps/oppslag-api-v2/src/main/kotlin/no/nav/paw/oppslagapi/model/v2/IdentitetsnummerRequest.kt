package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class IdentitetsnummerRequest(

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String
)