package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object HarBeskyttetAdresseFlaggtype: Flaggtype<HarBeskyttetadresseFlagg> {
    override val type: String = "har_gradert_adresse"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = HarBeskyttetadresseFlagg(verdi, tidspunkt)
}

data class HarBeskyttetadresseFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): LagretFlagg {
    override val type = HarBeskyttetAdresseFlaggtype
}