package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object OptOutFlaggtype: Flaggtype<OptOutFlag> {
    override val type: String = "opt_out"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = OptOutFlag(verdi, tidspunkt)
}

data class OptOutFlag(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): Flagg {
    override val type = OptOutFlaggtype
}