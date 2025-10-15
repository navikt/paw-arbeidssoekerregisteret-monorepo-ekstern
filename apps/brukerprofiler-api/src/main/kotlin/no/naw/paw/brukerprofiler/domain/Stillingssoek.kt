package no.naw.paw.brukerprofiler.domain

import no.naw.paw.brukerprofiler.api.ApiStillingssoek

sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

fun Stillingssoek.api(): ApiStillingssoek = when (this) {
    is ReiseveiSoek -> this.api()
    is StedSoek -> this.api()
}

interface HarSoekeord {
    val soekeord: List<String>
}
