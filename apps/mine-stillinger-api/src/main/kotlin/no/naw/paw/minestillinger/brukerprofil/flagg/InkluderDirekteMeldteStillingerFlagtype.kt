package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Instant

object InkluderDirekteMeldteStillingerFlagtype: Flaggtype<InkluderDirekteMeldteStillingerFlagg> {
    override val type: String = "inkluder_direktemeldte_stillinger"
    override fun flagg(verdi: Boolean, tidspunkt: Instant) = InkluderDirekteMeldteStillingerFlagg(verdi, tidspunkt)
}

data class InkluderDirekteMeldteStillingerFlagg(
    override val verdi: Boolean,
    override val tidspunkt: Instant
): LagretFlagg {
    override val type = InkluderDirekteMeldteStillingerFlagtype
}
