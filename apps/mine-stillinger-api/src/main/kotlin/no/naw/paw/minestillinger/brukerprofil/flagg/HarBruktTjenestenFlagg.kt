package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object HarBruktTjenestenFlaggtype: Flaggtype<HarBruktTjenestenFlagg> {
    override val type: String = "har_brukt_tjenesten"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarBruktTjenestenFlagg(verdi, tidspunkt)
}

data class HarBruktTjenestenFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): Flagg {
    override val type = HarBruktTjenestenFlaggtype
}
