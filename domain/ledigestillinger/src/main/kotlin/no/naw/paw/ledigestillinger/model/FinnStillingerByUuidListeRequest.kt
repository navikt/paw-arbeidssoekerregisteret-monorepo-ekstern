package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("BY_UUID_LISTE")
data class FinnStillingerByUuidListeRequest(

    @get:JsonProperty("type")
    override val type: FinnStillingerType,

    @get:JsonProperty("uuidListe")
    val uuidListe: List<UUID>

) : FinnStillingerRequest