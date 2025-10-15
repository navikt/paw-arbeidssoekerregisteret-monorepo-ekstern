package no.naw.paw.brukerprofiler.api.vo

enum class ApiStillingssoekType {
    STED_SOEK_V1,
    REISEVEI_SOEK_V1
}

fun ApiStillingssoekType.domain() = when (this) {
    ApiStillingssoekType.STED_SOEK_V1 -> no.naw.paw.brukerprofiler.domain.StillingssoekType.STED_SOEK_V1
    ApiStillingssoekType.REISEVEI_SOEK_V1 -> no.naw.paw.brukerprofiler.domain.StillingssoekType.REISEVEI_SOEK_V1
}
