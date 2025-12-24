package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asArbeidsgiverRowV2(): ArbeidsgiverRow = ArbeidsgiverRow(
    id = this[ArbeidsgivereTableV2.id].value,
    parentId = this[ArbeidsgivereTableV2.parentId],
    orgForm = this[ArbeidsgivereTableV2.orgForm],
    orgNr = this[ArbeidsgivereTableV2.orgNr],
    parentOrgNr = this[ArbeidsgivereTableV2.parentOrgNr],
    navn = this[ArbeidsgivereTableV2.navn],
    offentligNavn = this[ArbeidsgivereTableV2.offentligNavn]
)
