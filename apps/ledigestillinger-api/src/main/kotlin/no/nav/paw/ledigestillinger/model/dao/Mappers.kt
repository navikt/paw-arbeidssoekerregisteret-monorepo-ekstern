package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asStillingRow(): StillingRow = StillingRow(
    id = this[StillingerTable.id].value,
    uuid = this[StillingerTable.uuid],
    tittel = this[StillingerTable.tittel],
    beskrivelse = this[StillingerTable.beskrivelse],
    status = this[StillingerTable.status],
    kilde = this[StillingerTable.kilde],
    startDate = this[StillingerTable.startDate],
    annonseUrl = this[StillingerTable.annonseUrl],
    publisertTimestamp = this[StillingerTable.publisertTimestamp],
    utloeperTimestamp = this[StillingerTable.utloeperTimestamp],
    endretTimestamp = this[StillingerTable.endretTimestamp],
    metadata = this.asMetadataRow(),
    klassifiseringer = emptyList(), // TODO: Fiks
    arbeidsgivere = emptyList(), // TODO: Fiks
    beliggenheter = emptyList() // TODO: Fiks
)

fun ResultRow.asMetadataRow(): MetadataRow = MetadataRow(
    id = this[MetadataTable.id].value,
    parentId = this[MetadataTable.parentId],
    status = this[MetadataTable.status],
    recordTimestamp = this[MetadataTable.recordTimestamp],
    insertedTimestamp = this[MetadataTable.insertedTimestamp],
    updatedTimestamp = this[MetadataTable.updatedTimestamp]
)

fun ResultRow.asKlassifiseringRow(): KlassifiseringRow = KlassifiseringRow(
    id = this[KlassifiseringerTable.id].value,
    parentId = this[KlassifiseringerTable.parentId],
    type = this[KlassifiseringerTable.type],
    kode = this[KlassifiseringerTable.kode],
    navn = this[KlassifiseringerTable.navn]
)

fun ResultRow.asArbeidsgiverRow(): ArbeidsgiverRow = ArbeidsgiverRow(
    id = this[ArbeidsgivereTable.id].value,
    parentId = this[ArbeidsgivereTable.parentId],
    orgNr = this[ArbeidsgivereTable.orgNr],
    navn = this[ArbeidsgivereTable.navn],
    beskrivelse = this[ArbeidsgivereTable.beskrivelse]
)

fun ResultRow.asBeliggenhetRow(): BeliggenhetRow = BeliggenhetRow(
    id = this[BeliggenheterTable.id].value,
    parentId = this[BeliggenheterTable.parentId],
    adresse = this[BeliggenheterTable.adresse],
    postkode = this[BeliggenheterTable.postkode],
    poststed = this[BeliggenheterTable.poststed],
    kommune = this[BeliggenheterTable.kommune],
    fylke = this[BeliggenheterTable.fylke],
    land = this[BeliggenheterTable.land],
)
