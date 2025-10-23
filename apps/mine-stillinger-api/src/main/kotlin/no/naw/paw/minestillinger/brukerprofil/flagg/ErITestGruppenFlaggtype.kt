package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object ErITestGruppenFlaggtype: Flaggtype<ErITestGruppenFlagg> {
    override val type: String = "er_i_testgruppen"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = ErITestGruppenFlagg(verdi, tidspunkt)
}

data class ErITestGruppenFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): Flagg {
    override val type = ErITestGruppenFlaggtype
}