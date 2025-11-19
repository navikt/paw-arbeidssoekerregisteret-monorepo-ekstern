package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("PERIODE_ID")
data class PeriodeRequest(

    @field:JsonProperty("type")
    override val type: QueryType = QueryType.PERIODE_ID,

    @field:JsonProperty("periodeId")
    val periodeId: UUID
) : SingleQueryRequest