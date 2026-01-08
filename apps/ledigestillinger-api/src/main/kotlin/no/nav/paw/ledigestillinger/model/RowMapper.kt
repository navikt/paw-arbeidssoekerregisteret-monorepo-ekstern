package no.nav.paw.ledigestillinger.model

import no.nav.paw.ledigestillinger.model.dao.ArbeidsgiverRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonerTable
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.asStillingRow(
    arbeidsgiver: (Long) -> ArbeidsgiverRow?,
    kategorier: (Long) -> Iterable<KategoriRow>,
    klassifiseringer: (Long) -> Iterable<KlassifiseringRow>,
    lokasjoner: (Long) -> Iterable<LokasjonRow>,
    egenskaper: (Long) -> Iterable<EgenskapRow>
): StillingRow {
    val id = this[StillingerTable.id].value
    return StillingRow(
        id = id,
        uuid = this[StillingerTable.uuid],
        adnr = this[StillingerTable.adnr],
        tittel = this[StillingerTable.tittel],
        status = this[StillingerTable.status],
        visning = this[StillingerTable.visning],
        kilde = this[StillingerTable.kilde],
        medium = this[StillingerTable.medium],
        referanse = this[StillingerTable.referanse],
        arbeidsgivernavn = this[StillingerTable.arbeidsgivernavn],
        stillingstittel = this[StillingerTable.stillingstittel],
        ansettelsesform = this[StillingerTable.ansettelsesform],
        stillingsprosent = this[StillingerTable.stillingsprosent],
        stillingsantall = this[StillingerTable.stillingsantall],
        sektor = this[StillingerTable.sektor],
        soeknadsfrist = this[StillingerTable.soeknadsfrist],
        oppstartsfrist = this[StillingerTable.oppstartsfrist],
        opprettetTimestamp = this[StillingerTable.opprettetTimestamp],
        endretTimestamp = this[StillingerTable.endretTimestamp],
        publisertTimestamp = this[StillingerTable.publisertTimestamp],
        utloeperTimestamp = this[StillingerTable.utloeperTimestamp],
        messageTimestamp = this[StillingerTable.messageTimestamp],
        insertTimestamp = this[StillingerTable.insertTimestamp],
        updatedTimestamp = this[StillingerTable.updatedTimestamp],
        arbeidsgiver = arbeidsgiver(id),
        kategorier = kategorier(id),
        klassifiseringer = klassifiseringer(id),
        lokasjoner = lokasjoner(id),
        egenskaper = egenskaper(id)
    )
}

fun ResultRow.asArbeidsgiverRow(): ArbeidsgiverRow = ArbeidsgiverRow(
    id = this[ArbeidsgivereTable.id].value,
    parentId = this[ArbeidsgivereTable.parentId],
    orgForm = this[ArbeidsgivereTable.orgForm],
    orgNr = this[ArbeidsgivereTable.orgNr],
    parentOrgNr = this[ArbeidsgivereTable.parentOrgNr],
    navn = this[ArbeidsgivereTable.navn],
    offentligNavn = this[ArbeidsgivereTable.offentligNavn]
)

fun ResultRow.asKategoriRow(): KategoriRow = KategoriRow(
    id = this[KategorierTable.id].value,
    parentId = this[KategorierTable.parentId],
    kode = this[KategorierTable.kode],
    normalisertKode = this[KategorierTable.normalisertKode],
    navn = this[KategorierTable.navn]
)

fun ResultRow.asKlassifiseringRow(): KlassifiseringRow = KlassifiseringRow(
    id = this[KlassifiseringerTable.id].value,
    parentId = this[KlassifiseringerTable.parentId],
    type = this[KlassifiseringerTable.type],
    kode = this[KlassifiseringerTable.kode],
    navn = this[KlassifiseringerTable.navn]
)

fun ResultRow.asLokasjonRow(): LokasjonRow = LokasjonRow(
    id = this[LokasjonerTable.id].value,
    parentId = this[LokasjonerTable.parentId],
    adresse = this[LokasjonerTable.adresse],
    postkode = this[LokasjonerTable.postkode],
    poststed = this[LokasjonerTable.poststed],
    kommune = this[LokasjonerTable.kommune],
    kommunekode = this[LokasjonerTable.kommunekode],
    fylke = this[LokasjonerTable.fylke],
    fylkeskode = this[LokasjonerTable.fylkeskode],
    land = this[LokasjonerTable.land]
)

fun ResultRow.asEgenskapRow(): EgenskapRow = EgenskapRow(
    id = this[EgenskaperTable.id].value,
    parentId = this[EgenskaperTable.parentId],
    key = this[EgenskaperTable.key],
    value = this[EgenskaperTable.value]
)