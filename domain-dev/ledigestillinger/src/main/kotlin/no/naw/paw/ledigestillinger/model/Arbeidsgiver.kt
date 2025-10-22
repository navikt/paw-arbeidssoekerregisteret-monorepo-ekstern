package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Arbeidsgiver(

    @get:JsonProperty("orgForm")
    val orgForm: String,

    @get:JsonProperty("navn")
    val navn: String,

    @get:JsonProperty("offentligNavn")
    val offentligNavn: String,

    @get:JsonProperty("orgNr")
    val orgNr: String? = null,

    @get:JsonProperty("parentOrgNr")
    val parentOrgNr: String? = null

)