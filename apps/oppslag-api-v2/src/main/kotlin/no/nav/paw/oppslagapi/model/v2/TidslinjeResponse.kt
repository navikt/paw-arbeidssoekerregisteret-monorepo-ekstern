package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

data class TidslinjeResponse(

    @get:JsonProperty("tidslinjer")
    val tidslinjer: List<Tidslinje>? = null
)