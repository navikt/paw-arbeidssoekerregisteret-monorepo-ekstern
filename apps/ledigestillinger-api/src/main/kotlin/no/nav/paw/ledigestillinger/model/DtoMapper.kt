package no.nav.paw.ledigestillinger.model

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.paw.ledigestillinger.api.models.Arbeidsgiver
import no.nav.paw.ledigestillinger.api.models.Egenskap
import no.nav.paw.ledigestillinger.api.models.Frist
import no.nav.paw.ledigestillinger.api.models.FristType
import no.nav.paw.ledigestillinger.api.models.Kategori
import no.nav.paw.ledigestillinger.api.models.Klassifisering
import no.nav.paw.ledigestillinger.api.models.Lokasjon
import no.nav.paw.ledigestillinger.api.models.Sektor
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.api.models.Stillingsprosent
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgiverRow
import no.nav.paw.ledigestillinger.model.dao.EgenskapRow
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.nav.paw.ledigestillinger.util.fromUnformattedString

fun StillingRow.asDto(): Stilling = Stilling(
    uuid = uuid,
    adnr = adnr,
    tittel = tittel,
    status = status,
    arbeidsgivernavn = arbeidsgivernavn,
    stillingstittel = stillingstittel,
    ansettelsesform = ansettelsesform,
    stillingsprosent = stillingsprosent?.asStillingsprosent() ?: Stillingsprosent.UKJENT,
    stillingsantall = stillingsantall?.asAntall(),
    sektor = sektor?.asSektor() ?: Sektor.UKJENT,
    soeknadsfrist = soeknadsfrist?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = soeknadsfrist),
    oppstartsfrist = oppstartsfrist?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = oppstartsfrist),
    kategorier = kategorier.map { it.asDto() },
    lokasjoner = lokasjoner.map { it.asDto() },
    publisert = publisertTimestamp,
    utloeper = utloeperTimestamp,
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
    kode = kode,
    normalisertKode = normalisertKode,
    navn = navn
)

fun KlassifiseringRow.asDto(): Klassifisering = Klassifisering(
    type = this.type,
    kode = this.kode,
    navn = this.navn
)

fun LokasjonRow.asDto(): Lokasjon = Lokasjon(
    poststed = poststed,
    postkode = postkode,
    kommune = kommune,
    kommunenummer = kommunekode,
    fylke = fylke,
    fylkesnummer = fylkeskode,
    land = land
)

fun EgenskapRow.asDto(): Egenskap = Egenskap(
    key = key,
    value = value
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
        arbeidsgivernavn = this.businessName,
        arbeidsgiver = this.employer.asDto(),
        stillingstittel = jobtitle,
        ansettelsesform = engagementtype,
        stillingsprosent = extent?.asStillingsprosent() ?: Stillingsprosent.UKJENT,
        stillingsantall = positioncount?.asAntall(),
        sektor = this.properties?.find { it.key == "sector" }?.value?.asSektor() ?: Sektor.UKJENT,
        soeknadsfrist = applicationdue?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = applicationdue),
        oppstartsfrist = starttime?.asFrist() ?: Frist(type = FristType.UKJENT, verdi = starttime),
        kategorier = this.categories.map { it.asDto() },
        lokasjoner = this.locations.map { it.asDto() },
        utloeper = this.expires.fromLocalDateTimeString(),
        publisert = this.published.fromLocalDateTimeString()
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
    kode = this.styrkCode,
    normalisertKode = this.styrkCode.asNormalisertKode(),
    navn = this.name
)

fun Classification.asDto(): Klassifisering = Klassifisering(
    type = this.categoryType,
    kode = this.code,
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