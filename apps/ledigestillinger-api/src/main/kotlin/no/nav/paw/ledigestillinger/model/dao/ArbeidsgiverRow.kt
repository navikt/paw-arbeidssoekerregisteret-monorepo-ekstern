package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class ArbeidsgiverRow(
    val id: Long,
    val parentId: Long,
    val orgForm: String,
    val orgNr: String?,
    val parentOrgNr: String?,
    val navn: String,
    val offentligNavn: String
)

fun ResultRow.asArbeidsgiverRow(): ArbeidsgiverRow = ArbeidsgiverRow(
    id = this[ArbeidsgivereTable.id].value,
    parentId = this[ArbeidsgivereTable.parentId],
    orgForm = this[ArbeidsgivereTable.orgForm],
    orgNr = this[ArbeidsgivereTable.orgNr],
    parentOrgNr = this[ArbeidsgivereTable.parentOrgNr],
    navn = this[ArbeidsgivereTable.navn],
    offentligNavn = this[ArbeidsgivereTable.offentligNavn]
)
