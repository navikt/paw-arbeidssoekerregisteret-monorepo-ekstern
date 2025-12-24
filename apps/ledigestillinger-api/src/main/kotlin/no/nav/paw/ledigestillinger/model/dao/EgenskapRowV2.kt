package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asEgenskapRowV2(): EgenskapRow = EgenskapRow(
    id = this[EgenskaperTableV2.id].value,
    parentId = this[EgenskaperTableV2.parentId],
    key = this[EgenskaperTableV2.key],
    value = this[EgenskaperTableV2.value]
)
