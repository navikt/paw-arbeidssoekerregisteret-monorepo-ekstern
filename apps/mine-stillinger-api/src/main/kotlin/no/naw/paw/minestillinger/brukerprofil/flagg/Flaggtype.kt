package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant


sealed interface Flaggtype<A: Flagg> {
    val type: String
    fun flagg(verdi: Boolean, tidspunkt: Instant): Flagg
}

sealed interface Flagg {
    val type: Flaggtype<*>
    val verdi: Boolean
    val tidspunkt: Instant
}