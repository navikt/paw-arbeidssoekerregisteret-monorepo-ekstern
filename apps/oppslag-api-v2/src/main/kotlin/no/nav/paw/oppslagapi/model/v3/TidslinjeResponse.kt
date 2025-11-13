package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty

data class TidslinjeResponse(

    @get:JsonProperty("tidslinjer")
    val tidslinjer: List<Tidslinje> = emptyList()
)