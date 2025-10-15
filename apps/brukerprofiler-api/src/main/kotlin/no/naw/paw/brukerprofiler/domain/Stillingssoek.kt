package no.naw.paw.brukerprofiler.domain

sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

interface HarSoekeord {
    val soekeord: List<String>
}