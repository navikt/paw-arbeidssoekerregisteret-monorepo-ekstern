package no.nav.paw.ledigestillinger.model

import no.nav.paw.ledigestillinger.api.models.Kategori
import no.nav.paw.ledigestillinger.api.models.Lokasjon
import no.nav.paw.ledigestillinger.api.models.Sektor
import no.nav.paw.ledigestillinger.api.models.Soeknadsfrist
import no.nav.paw.ledigestillinger.api.models.SoeknadsfristType
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.util.fromUnformattedString

fun StillingRow.asDto(): Stilling = Stilling(
    uuid = uuid,
    adnr = adnr,
    tittel = tittel,
    status = status.asDto(),
    jobbtittel = egenskaper.find { it.key == "jobtitle" }?.value,
    ansettelsesform = egenskaper.find { it.key == "engagementtype" }?.value,
    ansettelsesprosent = egenskaper.find { it.key == "extent" }?.value,
    stillingsantall = egenskaper.find { it.key == "positioncount" }?.value,
    arbeidsgiver = arbeidsgiverNavn,
    sektor = egenskaper.find { it.key == "sector" }?.value?.asSektor() ?: Sektor.UKJENT,
    kategorier = kategorier.map { it.asDto() },
    lokasjoner = lokasjoner.map { it.asDto() },
    soeknadsfrist = egenskaper.find { it.key == "starttime" }?.value?.asSoeknadsfrist()
        ?: Soeknadsfrist(fristType = SoeknadsfristType.UKJENT),
    publisert = publisertTimestamp,
    utloeper = utloeperTimestamp,
)

fun StillingStatus.asDto(): no.nav.paw.ledigestillinger.api.models.StillingStatus = when (this) {
    StillingStatus.ACTIVE -> no.nav.paw.ledigestillinger.api.models.StillingStatus.AKTIV
    StillingStatus.INACTIVE -> no.nav.paw.ledigestillinger.api.models.StillingStatus.INAKTIV
    StillingStatus.STOPPED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.STOPPET
    StillingStatus.DELETED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.SLETTET
    StillingStatus.REJECTED -> no.nav.paw.ledigestillinger.api.models.StillingStatus.AVVIST
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

fun LokasjonRow.asDto(): Lokasjon = Lokasjon(
    poststed = poststed,
    postkode = postkode,
    kommune = kommune,
    kommunenummer = kommunekode,
    fylke = fylke,
    fylkesnummer = fylkeskode,
    land = land
)

fun String.asSoeknadsfrist(): Soeknadsfrist = when {
    this.lowercase().contains("snar") -> Soeknadsfrist(
        raw = this,
        fristType = SoeknadsfristType.SNAREST
    )

    this.matches(Regex("^[0-9]+.*")) -> Soeknadsfrist(
        raw = this,
        fristType = SoeknadsfristType.DATO,
        dato = this.fromUnformattedString()
    )

    else -> Soeknadsfrist(
        raw = this,
        fristType = SoeknadsfristType.UKJENT
    )
}