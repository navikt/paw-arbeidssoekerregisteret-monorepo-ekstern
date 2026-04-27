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
import no.naw.paw.ledigestillinger.model.TekniskTag
import no.naw.paw.ledigestillinger.model.VisningGrad
import java.time.Instant
import java.util.*

fun Message<UUID, Ad>.asStillingRow(): StillingRow {
    with(this) {
        val (tags, tekniskeTags) = value.asTags()
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
            kategorier = value.categories?.map { it.asKategoriRow() } ?: emptyList(),
            klassifiseringer = value.classifications?.map { it.asKlassifiseringRow() } ?: emptyList(),
            lokasjoner = value.locations?.map { it.asLokasjonRow() } ?: emptyList(),
            egenskaper = value.asEgenskapRows(),
            tags = tags,
            tekniskeTags = tekniskeTags
        )
    }
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

fun Ad.asEgenskapRows(): Iterable<EgenskapRow> {
    val egenskapRows = mutableListOf<EgenskapRow>()
    if (properties != null) {
        properties
            .map { it.asEgenskapRow() }
            .forEach(egenskapRows::add)
    }
    if (eksperimentelleProperties != null) {
        eksperimentelleProperties
            .map { it.asEgenskapRow() }
            .forEach(egenskapRows::add)
    }
    return egenskapRows
}

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

fun Ad.asTags(): Pair<Set<Tag>, Set<TekniskTag>> {
    val tags = mutableSetOf<Tag>()
    val tekniskeTags = mutableSetOf<TekniskTag>()

    val arbeidserfaring = this.eksperimentelleProperties
        .find { it.key.equals("experience", ignoreCase = true) }?.value
    if (arbeidserfaring != null) {
        if (arbeidserfaring.ingenKravTilArbeidserfaring()) {
            tags.add(Tag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1)
        } else if (arbeidserfaring.harKravTilArbeidserfaring()) {
            tags.add(Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1)
        } else {
            tekniskeTags.add(TekniskTag.UKJENT_KRAV_TIL_ARBEIDSERFARING_V1)
        }
    }

    val utdanning = this.eksperimentelleProperties
        .find { it.key.equals("education", ignoreCase = true) }?.value
    if (utdanning != null) {
        if (utdanning.ingenKravTilUtdanning()) {
            tags.add(Tag.INGEN_KRAV_TIL_UTDANNING_V1)
        } else if (utdanning.harKravTilUtdanning()) {
            tags.add(Tag.HAR_KRAV_TIL_UTDANNING_V1)
        } else {
            tekniskeTags.add(TekniskTag.UKJENT_KRAV_TIL_UTDANNING_V1)
        }
    }

    val kreverFoererkort = this.eksperimentelleProperties
        .find { it.key.equals("needDriversLicense", ignoreCase = true) }?.value
    if (kreverFoererkort != null) {
        if (kreverFoererkort.contains("true", ignoreCase = true)) {
            tags.add(Tag.HAR_KRAV_TIL_FOERERKORT_V1)
        } else if (kreverFoererkort.contains("false", ignoreCase = true)) {
            tags.add(Tag.INGEN_KRAV_TIL_FOERERKORT_V1)
        } else {
            tekniskeTags.add(TekniskTag.UKJENT_KRAV_TIL_FOERERKORT_V1)
        }
    }

    val sourceErDir = this.source.equals("DIR", ignoreCase = true)
    val direktemeldtStillingskategori = this.properties
        .find { it.key.equals("direktemeldtStillingskategori", ignoreCase = true) }?.value ?: "null"

    if (sourceErDir && direktemeldtStillingskategori.equals("STILLING", ignoreCase = true)) {
        tags.add(Tag.DIREKTEMELDT_V1)
    }

    Span.current()
        .addEvent(
            "beregnet_tags",
            Attributes.builder()
                .put(stringKey("source"), this.source)
                .put(stringKey("experience"), arbeidserfaring ?: "null")
                .put(stringKey("education"), utdanning ?: "null")
                .put(stringKey("needDriversLicense"), kreverFoererkort ?: "null")
                .put(stringKey("direktemeldtStillingskategori"), direktemeldtStillingskategori)
                .put(stringKey("tags"), tags.joinToString(","))
                .put(stringKey("ad_uuid"), this.uuid.toString())
                .build()
        )

    return tags to tekniskeTags
}

private fun String.ingenKravTilArbeidserfaring(): Boolean {
    return this.contains("Ingen", ignoreCase = true)
}

private fun String.harKravTilArbeidserfaring(): Boolean {
    return listOf(
        "Noe",
        "Mye"
    ).any { this.contains(it, ignoreCase = true) }
}

private fun String.ingenKravTilUtdanning(): Boolean {
    return this.contains("Ingen", ignoreCase = true)
}

private fun String.harKravTilUtdanning(): Boolean {
    return listOf(
        "Videregående",
        "Fagbrev",
        "Fagskole",
        "Bachelor",
        "Master",
        "Forskningsgrad"
    ).any { this.contains(it, ignoreCase = true) }
}