package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object TjenestenErAktivFlaggtype: Flaggtype<TjenestenErAktivFlagg> {
    override val type: String = "tjenesten_er_aktiv"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = TjenestenErAktivFlagg(verdi, tidspunkt)
}

data class TjenestenErAktivFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): Flagg {
    override val type = TjenestenErAktivFlaggtype
}