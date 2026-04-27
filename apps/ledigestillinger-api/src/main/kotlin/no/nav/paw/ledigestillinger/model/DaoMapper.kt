package no.nav.paw.ledigestillinger.model

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
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
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.naw.paw.ledigestillinger.model.KlassifiseringType
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.SortOrder
import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.ledigestillinger.model.VisningGrad
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
        egenskaper = value.properties?.map { it.asEgenskapRow() } ?: listOf(),
        tags = value.asTags()
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
    type = this.categoryType.asKlassifiseringType(),
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

fun String.asKlassifiseringType(): KlassifiseringType = when (this) {
    "ESCO" -> KlassifiseringType.ESCO
    "JANZZ" -> KlassifiseringType.JANZZ
    "STYRK08" -> KlassifiseringType.STYRK08
    "STYRK08NAV" -> KlassifiseringType.STYRK08NAV
    else -> throw IllegalArgumentException("Ukjent type: $this")
}

fun String.asNormalisertKode(): String {
    val match = Regex("^(\\d+)(?:\\..*)?$").find(this)
    return if (match == null || match.groupValues.isEmpty()) {
        this
    } else {
        match.groupValues[1]
    }
}

const val ARBEIDSERFARING_KEY = "experience"
const val UTDANNING_KEY = "education"
const val KREVER_FOERERKORT_KEY = "needDriversLicense"
const val DIREKTEMELDT_STILLINGSKATEGORI_KEY = "direktemeldtStillingskategori"

fun Ad.asTags(): Set<Tag> {
    val tags = mutableSetOf<Tag>()

    val arbeidserfaring = this.properties
        .find { it.key.equals(ARBEIDSERFARING_KEY, ignoreCase = true) }?.value
    if (arbeidserfaring == null) {
        tags.add(Tag.UKJENT_KRAV_TIL_ARBEIDSERFARING_V1)
    } else if (arbeidserfaring.contains("Ingen", ignoreCase = true)) {
        tags.add(Tag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1)
    } else {
        tags.add(Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1)
    }

    val utdanning = this.properties
        .find { it.key.equals(UTDANNING_KEY, ignoreCase = true) }?.value
    if (utdanning == null) {
        tags.add(Tag.UKJENT_KRAV_TIL_UTDANNING_V1)
    } else if (utdanning.contains("Ingen", ignoreCase = true)) {
        tags.add(Tag.INGEN_KRAV_TIL_UTDANNING_V1)
    } else {
        tags.add(Tag.HAR_KRAV_TIL_UTDANNING_V1)
    }

    val kreverFoererkort = this.properties
        .find { it.key.equals(KREVER_FOERERKORT_KEY, ignoreCase = true) }?.value
    if (kreverFoererkort == null) {
        tags.add(Tag.UKJENT_KRAV_TIL_FOERERKORT_V1)
    } else if (kreverFoererkort.contains("true", ignoreCase = true)) {
        tags.add(Tag.HAR_KRAV_TIL_FOERERKORT_V1)
    } else if (kreverFoererkort.contains("false", ignoreCase = true)) {
        tags.add(Tag.INGEN_KRAV_TIL_FOERERKORT_V1)
    } else {
        tags.add(Tag.UKJENT_KRAV_TIL_FOERERKORT_V1)
    }

    val sourceErDir = this.source.equals("DIR", ignoreCase = true)
    val dirmeldtStillingKategori = this.properties
        .find { it.key.equals(DIREKTEMELDT_STILLINGSKATEGORI_KEY, ignoreCase = true) }?.value ?: "null"

    if (sourceErDir && dirmeldtStillingKategori.equals("STILLING", ignoreCase = true)) {
        tags.add(Tag.DIREKTEMELDT_V1)
    }

    Span.current()
        .addEvent(
            "beregnet_tags",
            Attributes.builder()
                .put(stringKey("source"), this.source)
                .put(stringKey(ARBEIDSERFARING_KEY), arbeidserfaring ?: "null")
                .put(stringKey(UTDANNING_KEY), utdanning ?: "null")
                .put(stringKey(KREVER_FOERERKORT_KEY), kreverFoererkort ?: "null")
                .put(stringKey(DIREKTEMELDT_STILLINGSKATEGORI_KEY), dirmeldtStillingKategori)
                .put(stringKey("tags"), tags.joinToString(","))
                .put(stringKey("ad_uuid"), this.uuid.toString())
                .build()
        )

    return tags
}