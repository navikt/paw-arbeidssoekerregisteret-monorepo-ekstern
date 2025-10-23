package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object HarGodeMuligheterFlaggtype: Flaggtype<HarGodeMuligheterFlagg> {
    override val type: String = "har_gode_muligheter"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarGodeMuligheterFlagg(verdi, tidspunkt)
}

data class HarGodeMuligheterFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): Flagg {
    override val type = HarGodeMuligheterFlaggtype
}