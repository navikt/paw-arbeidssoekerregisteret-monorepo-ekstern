package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("PERIODE")
data class PeriodeRequest(

    @get:JsonProperty("type")
    override val type: QueryType,

    @get:JsonProperty("periodeId")
    val periodeId: UUID
) : SingleQueryRequest