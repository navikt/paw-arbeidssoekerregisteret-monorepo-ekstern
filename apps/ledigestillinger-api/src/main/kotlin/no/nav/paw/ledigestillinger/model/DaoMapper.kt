package no.nav.paw.ledigestillinger.model

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.PrivacyChannel
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgiverRow
import no.nav.paw.ledigestillinger.model.dao.BeliggenhetRow
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromIsoString
import java.time.Instant
import java.util.*

fun Message<UUID, Ad>.asStillingRow(): StillingRow {
    val value = this.value
    return StillingRow(
        id = -1,
        uuid = UUID.fromString(value.uuid),
        adnr = value.adnr,
        tittel = value.title,
        status = value.status.asStillingStatus(),
        visning = value.privacy.asVisningGrad(),
        kilde = value.source,
        medium = value.medium,
        referanse = value.reference,
        arbeidsgiverNavn = value.businessName,
        opprettetTimestamp = value.created.fromIsoString(),
        endretTimestamp = value.updated.fromIsoString(),
        publisertTimestamp = value.published.fromIsoString(),
        utloeperTimestamp = value.expires?.fromIsoString(),
        messageTimestamp = this.timestamp,
        insertTimestamp = Instant.now(),
        updatedTimestamp = Instant.now(),
        arbeidsgiver = value.employer?.asArbeidsgiverRow(),
        kategorier = value.categories?.map { it.asKategoriRow() } ?: listOf(),
        klassifiseringer = value.classifications?.map { it.asKlassifiseringRow() } ?: listOf(),
        beliggenheter = value.locations?.map { it.asBeliggenhetRow() } ?: listOf(),
        egenskaper = value.properties?.map { it.asEgenskapRow() } ?: listOf()
    )
}

fun AdStatus.asStillingStatus(): StillingStatus = when (this) {
    AdStatus.ACTIVE -> StillingStatus.ACTIVE
    AdStatus.STOPPED -> StillingStatus.STOPPED
    AdStatus.REJECTED -> StillingStatus.REJECTED
    AdStatus.INACTIVE -> StillingStatus.INACTIVE
    AdStatus.DELETED -> StillingStatus.DELETED
}

fun PrivacyChannel.asVisningGrad(): VisningGrad = when (this) {
    PrivacyChannel.SHOW_ALL -> VisningGrad.SHOW_ALL
    PrivacyChannel.INTERNAL_NOT_SHOWN -> VisningGrad.INTERNAL_NOT_SHOWN
    PrivacyChannel.DONT_SHOW_EMPLOYER -> VisningGrad.DONT_SHOW_EMPLOYER
    PrivacyChannel.DONT_SHOW_AUTHOR -> VisningGrad.DONT_SHOW_AUTHOR
}

fun Company.asArbeidsgiverRow(): ArbeidsgiverRow = ArbeidsgiverRow(
    id = -1,
    parentId = -1,
    orgForm = orgform,
    orgNr = orgnr,
    parentOrgNr = parentOrgnr,
    navn = name,
    offentligNavn = publicName
)

fun StyrkCategory.asKategoriRow(): KategoriRow = KategoriRow(
    id = -1,
    parentId = -1,
    kode = this.styrkCode,
    navn = this.name
)

fun Classification.asKlassifiseringRow(): KlassifiseringRow = KlassifiseringRow(
    id = -1,
    parentId = -1,
    type = this.categoryType,
    kode = this.code,
    navn = this.name
)

fun Location.asBeliggenhetRow(): BeliggenhetRow = BeliggenhetRow(
    id = -1,
    parentId = -1,
    adresse = this.address,
    postkode = this.postalCode,
    poststed = this.city,
    kommune = this.municipal,
    kommunekode = this.municipalCode,
    fylke = this.county,
    fylkeskode = this.countyCode,
    land = this.country
)

fun Property.asEgenskapRow(): EgenskapRow = EgenskapRow(
    id = -1,
    parentId = -1,
    key = this.key,
    value = this.value
)