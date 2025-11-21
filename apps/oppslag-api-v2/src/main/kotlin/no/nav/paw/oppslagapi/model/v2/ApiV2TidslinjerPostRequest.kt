package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiV2TidslinjerPostRequest(

    @get:JsonProperty("perioder")
    val perioder: List<java.util.UUID>,

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String
)