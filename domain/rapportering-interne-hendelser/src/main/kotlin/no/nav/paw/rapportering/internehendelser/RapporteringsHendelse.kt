package no.nav.paw.rapportering.internehendelser

import java.util.*

interface RapporteringsHendelse {
    val hendelseType: String
    val hendelseId: UUID
    val periodeId: UUID
    val identitetsnummer: String
}

