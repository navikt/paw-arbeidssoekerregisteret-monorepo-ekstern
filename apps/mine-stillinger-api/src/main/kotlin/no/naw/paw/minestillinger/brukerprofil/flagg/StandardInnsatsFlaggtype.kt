package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object StandardInnsatsFlaggtype: Flaggtype<StandardInnsatsFlagg> {
    override val type: String = "standard_innsats"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = StandardInnsatsFlagg(verdi, tidspunkt)
}

data class StandardInnsatsFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): LagretFlagg {
    override val type = StandardInnsatsFlaggtype
}