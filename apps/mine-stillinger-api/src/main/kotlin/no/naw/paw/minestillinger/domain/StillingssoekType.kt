package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType

enum class StillingssoekType {
    STED_SOEK_V1,
    REISEVEI_SOEK_V1
}

fun ApiStillingssoekType.domain(): StillingssoekType {
    return when (this) {
        ApiStillingssoekType.STED_SOEK_V1 -> StillingssoekType.STED_SOEK_V1
        ApiStillingssoekType.REISEVEI_SOEK_V1 -> StillingssoekType.REISEVEI_SOEK_V1
    }
}

fun StillingssoekType.api(): ApiStillingssoekType {
    return when (this) {
        StillingssoekType.STED_SOEK_V1 -> ApiStillingssoekType.STED_SOEK_V1
        StillingssoekType.REISEVEI_SOEK_V1 -> ApiStillingssoekType.REISEVEI_SOEK_V1
    }
}