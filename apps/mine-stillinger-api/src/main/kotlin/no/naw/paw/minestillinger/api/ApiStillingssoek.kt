package no.naw.paw.minestillinger.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType
import no.naw.paw.minestillinger.domain.Stillingssoek

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "soekType",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(ApiStedSoek::class, name = "STED_SOEK_V1"),
    JsonSubTypes.Type(ApiReiseveiSoek::class, name = "REISEVEI_SOEK_V1")
)
sealed interface ApiStillingssoek {
    val soekType: ApiStillingssoekType
}

interface ApiHarSoekeord {
    val soekeord: List<String>
}

fun ApiStillingssoek.domain(): Stillingssoek =
    when (this) {
        is ApiReiseveiSoek -> this.domain()
        is ApiStedSoek -> this.domain()
    }

