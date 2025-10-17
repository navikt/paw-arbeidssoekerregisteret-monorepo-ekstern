package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.ApiStillingssoek

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
