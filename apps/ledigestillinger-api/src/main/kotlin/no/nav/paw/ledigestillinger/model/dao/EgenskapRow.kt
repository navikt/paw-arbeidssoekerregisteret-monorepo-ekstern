package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

data class EgenskapRow(
    val id: Long,
    val parentId: Long,
    val key: String,
    val value: String
)

fun ResultRow.asEgenskapRow(): EgenskapRow = EgenskapRow(
    id = this[EgenskaperTable.id].value,
    parentId = this[EgenskaperTable.parentId],
    key = this[EgenskaperTable.key],
    value = this[EgenskaperTable.value]
)
