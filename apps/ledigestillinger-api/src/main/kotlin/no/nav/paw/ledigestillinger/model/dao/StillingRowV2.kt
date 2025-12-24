package no.nav.paw.ledigestillinger.model.dao

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asStillingRowV2(
    arbeidsgiver: (Long) -> ArbeidsgiverRow?,
    kategorier: (Long) -> Iterable<KategoriRow>,
    klassifiseringer: (Long) -> Iterable<KlassifiseringRow>,
    lokasjoner: (Long) -> Iterable<LokasjonRow>,
    egenskaper: (Long) -> Iterable<EgenskapRow>
): StillingRow {
    val id = this[StillingerTableV2.id].value
    return StillingRow(
        id = id,
        uuid = this[StillingerTableV2.uuid],
        adnr = this[StillingerTableV2.adnr],
        tittel = this[StillingerTableV2.tittel],
        status = this[StillingerTableV2.status],
        visning = this[StillingerTableV2.visning],
        kilde = this[StillingerTableV2.kilde],
        medium = this[StillingerTableV2.medium],
        referanse = this[StillingerTableV2.referanse],
        arbeidsgivernavn = this[StillingerTableV2.arbeidsgivernavn],
        stillingstittel = this[StillingerTableV2.stillingstittel],
        ansettelsesform = this[StillingerTableV2.ansettelsesform],
        stillingsprosent = this[StillingerTableV2.stillingsprosent],
        stillingsantall = this[StillingerTableV2.stillingsantall],
        sektor = this[StillingerTableV2.sektor],
        soeknadsfrist = this[StillingerTableV2.soeknadsfrist],
        oppstartsfrist = this[StillingerTableV2.oppstartsfrist],
        opprettetTimestamp = this[StillingerTableV2.opprettetTimestamp],
        endretTimestamp = this[StillingerTableV2.endretTimestamp],
        publisertTimestamp = this[StillingerTableV2.publisertTimestamp],
        utloeperTimestamp = this[StillingerTableV2.utloeperTimestamp],
        messageTimestamp = this[StillingerTableV2.messageTimestamp],
        insertTimestamp = this[StillingerTableV2.insertTimestamp],
        updatedTimestamp = this[StillingerTableV2.updatedTimestamp],
        arbeidsgiver = arbeidsgiver(id),
        kategorier = kategorier(id),
        klassifiseringer = klassifiseringer(id),
        lokasjoner = lokasjoner(id),
        egenskaper = egenskaper(id)
    )
}