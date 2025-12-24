package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asKlassifiseringRowV2(): KlassifiseringRow = KlassifiseringRow(
    id = this[KlassifiseringerTableV2.id].value,
    parentId = this[KlassifiseringerTableV2.parentId],
    type = this[KlassifiseringerTableV2.type],
    kode = this[KlassifiseringerTableV2.kode],
    navn = this[KlassifiseringerTableV2.navn]
)