package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object HarGradertAdresseFlaggtype: Flaggtype<HarGradertAdresseFlagg> {
    override val type: String = "har_gradert_adresse"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarGradertAdresseFlagg(verdi, tidspunkt)
}

data class HarGradertAdresseFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): LagretFlagg {
    override val type = HarGradertAdresseFlaggtype
}