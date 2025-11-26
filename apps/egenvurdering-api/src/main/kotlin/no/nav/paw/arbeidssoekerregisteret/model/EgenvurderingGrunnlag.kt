package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty

data class EgenvurderingGrunnlag(

    @get:JsonProperty("grunnlag")
    val grunnlag: Profilering? = null

)