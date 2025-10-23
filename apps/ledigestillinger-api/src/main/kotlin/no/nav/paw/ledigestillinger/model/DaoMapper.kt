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
import no.nav.paw.ledigestillinger.api.models.Paging
import no.nav.paw.ledigestillinger.api.models.SortOrder
import no.nav.paw.ledigestillinger.api.models.StillingStatus
import no.nav.paw.ledigestillinger.api.models.VisningGrad
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgiverRow
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
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
        arbeidsgivernavn = value.businessName,
        stillingstittel = value.properties?.find { it.key == "jobtitle" }?.value,
        ansettelsesform = value.properties?.find { it.key == "engagementtype" }?.value,
        stillingsprosent = value.properties?.find { it.key == "extent" }?.value,
        stillingsantall = value.properties?.find { it.key == "positioncount" }?.value,
        sektor = value.properties?.find { it.key == "sector" }?.value,
        soeknadsfrist = value.properties?.find { it.key == "applicationdue" }?.value,
        oppstartsfrist = value.properties?.find { it.key == "starttime" }?.value,
        opprettetTimestamp = value.created.fromLocalDateTimeString(),
        endretTimestamp = value.updated.fromLocalDateTimeString(),
        publisertTimestamp = value.published.fromLocalDateTimeString(),
        utloeperTimestamp = value.expires?.fromLocalDateTimeString(),
        messageTimestamp = this.timestamp,
        insertTimestamp = Instant.now(),
        updatedTimestamp = Instant.now(),
        arbeidsgiver = value.employer?.asArbeidsgiverRow(),
        kategorier = value.categories?.map { it.asKategoriRow() } ?: listOf(),
        klassifiseringer = value.classifications?.map { it.asKlassifiseringRow() } ?: listOf(),
        lokasjoner = value.locations?.map { it.asLokasjonRow() } ?: listOf(),
        egenskaper = value.properties?.map { it.asEgenskapRow() } ?: listOf()
    )
}

fun AdStatus.asStillingStatus(): StillingStatus = when (this) {
    AdStatus.ACTIVE -> StillingStatus.AKTIV
    AdStatus.STOPPED -> StillingStatus.STOPPET
    AdStatus.REJECTED -> StillingStatus.AVVIST
    AdStatus.INACTIVE -> StillingStatus.INAKTIV
    AdStatus.DELETED -> StillingStatus.SLETTET
}

fun PrivacyChannel.asVisningGrad(): VisningGrad = when (this) {
    PrivacyChannel.SHOW_ALL -> VisningGrad.UBEGRENSET
    PrivacyChannel.INTERNAL_NOT_SHOWN -> VisningGrad.BEGRENSET_INTERNT
    PrivacyChannel.DONT_SHOW_EMPLOYER -> VisningGrad.BEGRENSET_ARBEIDSGIVER
    PrivacyChannel.DONT_SHOW_AUTHOR -> VisningGrad.BEGRENSET_KILDE
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
    normalisertKode = this.styrkCode.asNormalisertKode(),
    navn = this.name
)

fun Classification.asKlassifiseringRow(): KlassifiseringRow = KlassifiseringRow(
    id = -1,
    parentId = -1,
    type = this.categoryType,
    kode = this.code,
    navn = this.name
)

fun Location.asLokasjonRow(): LokasjonRow = LokasjonRow(
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

fun Paging.order(): org.jetbrains.exposed.v1.core.SortOrder = when (this.sortOrder) {
    SortOrder.ASC -> org.jetbrains.exposed.v1.core.SortOrder.ASC
    SortOrder.DESC -> org.jetbrains.exposed.v1.core.SortOrder.DESC
}

fun Paging.size(): Int {
    return if (this.pageSize < 1) 10 else this.pageSize
}

fun Paging.offset(): Long {
    return if (page < 1) 0 else (page - 1) * size().toLong()
}

fun String.asNormalisertKode(): String {
    val match = Regex("^(\\d+)(?:\\..*)?$").find(this)
    return if (match == null || match.groupValues.isEmpty()) {
        this
    } else {
        match.groupValues[1]
    }
}