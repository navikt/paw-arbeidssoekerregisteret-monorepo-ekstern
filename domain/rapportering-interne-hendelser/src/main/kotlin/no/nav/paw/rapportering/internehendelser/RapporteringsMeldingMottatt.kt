package no.nav.paw.rapportering.internehendelser

import java.util.*

const val meldingMottattHendelseType = "rapportering.melding_mottatt"

data class RapporteringsMeldingMottatt(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val rapporteringsId: UUID,
    val fortsetterSomArbeidssoker: Boolean
) : RapporteringsHendelse {
    override val hendelseType: String = meldingMottattHendelseType
}