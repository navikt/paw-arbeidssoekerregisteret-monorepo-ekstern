package no.nav.paw.ledigestillinger.model

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgiverRow
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.nav.paw.ledigestillinger.util.fromUnformattedString
import no.naw.paw.ledigestillinger.model.Arbeidsgiver
import no.naw.paw.ledigestillinger.model.Egenskap
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import no.naw.paw.ledigestillinger.model.Kategori
import no.naw.paw.ledigestillinger.model.Klassifisering
import no.naw.paw.ledigestillinger.model.KlassifiseringType
import no.naw.paw.ledigestillinger.model.Lokasjon
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.ledigestillinger.model.Sektor
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.Stillingsprosent
import no.naw.paw.ledigestillinger.model.StyrkKode

fun StillingRow.asDto(): Stilling = Stilling(
    uuid = this.uuid,
    adnr = this.adnr,
    tittel = this.tittel,
    status = this.status,
    visning = this.visning,
    arbeidsgivernavn = this.arbeidsgivernavn,
    arbeidsgiver = this.arbeidsgiver?.asDto(),
    stillingstittel = this.stillingstittel,
    ansettelsesform = this.ansettelsesform,
    stillingsprosent = this.stillingsprosent?.asStillingsprosent() ?: Stillingsprosent.UKJENT,
    stillingsantall = this.stillingsantall?.asAntall(),
    sektor = this.sektor?.asSektor() ?: Sektor.UKJENT,
    soeknadsfrist = this.soeknadsfrist?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = this.soeknadsfrist),
    oppstartsfrist = this.oppstartsfrist?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = this.oppstartsfrist),
    styrkkoder = (this.kategorier.map { it.asStyrkDto() } + this.klassifiseringer
        .filter { it.type == KlassifiseringType.STYRK08 }
        .map { it.asStyrkDto() })
        .distinctBy { it.kode },
    lokasjoner = this.lokasjoner.map { it.asDto() },
    publisert = this.publisertTimestamp,
    utloeper = this.utloeperTimestamp,
)

fun ArbeidsgiverRow.asDto(): Arbeidsgiver = Arbeidsgiver(
    orgForm = this.orgForm,
    orgNr = this.orgNr,
    parentOrgNr = this.parentOrgNr,
    navn = this.navn,
    offentligNavn = this.offentligNavn
)

fun String.asStillingsprosent(): Stillingsprosent = when {
    this.lowercase().contains("heltid") -> Stillingsprosent.HELTID
    this.lowercase().contains("deltid") -> Stillingsprosent.DELTID
    else -> Stillingsprosent.UKJENT
}

fun String.asAntall(): Int? = when {
    this.trim().matches(Regex("[0-9]+")) -> this.toInt()
    else -> null
}

fun String.asSektor(): Sektor = when {
    this.lowercase().contains("privat") -> Sektor.PRIVAT
    this.lowercase().contains("offentlig") -> Sektor.OFFENTLIG
    else -> Sektor.UKJENT
}

fun KategoriRow.asDto(): Kategori = Kategori(
    kode = this.normalisertKode,
    navn = this.navn
)

fun KategoriRow.asStyrkDto(): StyrkKode = StyrkKode(
    kode = this.normalisertKode,
    navn = this.navn
)

fun KlassifiseringRow.asDto(): Klassifisering = Klassifisering(
    type = this.type,
    kode = this.kode,
    navn = this.navn
)

fun KlassifiseringRow.asStyrkDto(): StyrkKode = StyrkKode(
    kode = this.kode,
    navn = this.navn
)

fun LokasjonRow.asDto(): Lokasjon = Lokasjon(
    poststed = this.poststed,
    postkode = this.postkode,
    kommune = this.kommune,
    kommunenummer = this.kommunekode,
    fylke = this.fylke,
    fylkesnummer = this.fylkeskode,
    land = this.land
)

fun EgenskapRow.asDto(): Egenskap = Egenskap(
    key = this.key,
    value = this.value
)

fun String.asFrist(): Frist = when {
    this.lowercase().matches(Regex(".*(snart|snarest).*")) -> Frist(
        verdi = this,
        type = FristType.SNAREST
    )

    this.lowercase().matches(Regex(".*(fortløpende).*")) -> Frist(
        verdi = this,
        type = FristType.FORTLOEPENDE
    )

    this.trim().matches(Regex("[0-9]+.*")) -> {
        val dato = this.trim().fromUnformattedString()
        if (dato == null) {
            Frist(
                verdi = this,
                type = FristType.UKJENT
            )
        } else {
            Frist(
                verdi = this,
                type = FristType.DATO,
                dato = this.trim().fromUnformattedString()
            )
        }
    }

    else -> Frist(
        verdi = this,
        type = FristType.UKJENT
    )
}

fun Ad.asDto(): Stilling {
    val jobtitle = this.properties?.find { it.key == "jobtitle" }?.value
    val engagementtype = this.properties?.find { it.key == "engagementtype" }?.value
    val extent = this.properties?.find { it.key == "extent" }?.value
    val positioncount = this.properties?.find { it.key == "positioncount" }?.value
    val applicationdue = this.properties?.find { it.key == "applicationdue" }?.value
    val starttime = this.properties?.find { it.key == "starttime" }?.value
    return Stilling(
        uuid = this.uuid.let { java.util.UUID.fromString(it) },
        adnr = this.adnr,
        tittel = this.title,
        status = this.status.asStillingStatus(),
        visning = this.privacy.asVisningGrad(),
        arbeidsgivernavn = this.businessName,
        arbeidsgiver = this.employer.asDto(),
        stillingstittel = jobtitle,
        ansettelsesform = engagementtype,
        stillingsprosent = extent?.asStillingsprosent() ?: Stillingsprosent.UKJENT,
        stillingsantall = positioncount?.asAntall(),
        sektor = this.properties?.find { it.key == "sector" }?.value?.asSektor() ?: Sektor.UKJENT,
        soeknadsfrist = applicationdue?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = applicationdue),
        oppstartsfrist = starttime?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = starttime),
        styrkkoder = (this.categories.map { it.asStyrkDto() } + this.classifications
            .filter { it.categoryType.asKlassifiseringType() == KlassifiseringType.STYRK08 }
            .map { it.asStyrkDto() }).distinctBy { it.kode },
        lokasjoner = this.locations.map { it.asDto() },
        publisert = this.published.fromLocalDateTimeString(),
        utloeper = this.expires?.fromLocalDateTimeString()
    )
}

fun Company.asDto(): Arbeidsgiver = Arbeidsgiver(
    orgForm = this.orgform,
    orgNr = this.orgnr,
    parentOrgNr = this.parentOrgnr,
    navn = this.name,
    offentligNavn = this.publicName
)

fun StyrkCategory.asDto(): Kategori = Kategori(
    kode = this.styrkCode.asNormalisertKode(),
    navn = this.name
)

fun StyrkCategory.asStyrkDto(): StyrkKode = StyrkKode(
    kode = this.styrkCode.asNormalisertKode(),
    navn = this.name
)

fun Classification.asDto(): Klassifisering = Klassifisering(
    type = this.categoryType.asKlassifiseringType(),
    kode = this.code,
    navn = this.name
)

fun Classification.asStyrkDto(): StyrkKode = StyrkKode(
    kode = this.code.asNormalisertKode(),
    navn = this.name
)

fun Location.asDto(): Lokasjon = Lokasjon(
    poststed = this.city,
    postkode = this.postalCode,
    kommune = this.municipal,
    kommunenummer = this.municipalCode,
    fylke = this.county,
    fylkesnummer = this.countyCode,
    land = this.country
)

fun Property.asDto(): Egenskap = Egenskap(
    key = this.key,
    value = this.value
)

fun Paging.asResponse(hitSize: Int): PagingResponse = PagingResponse(
    page = this.page,
    pageSize = this.pageSize,
    hitSize = hitSize,
    sortOrder = this.sortOrder
)