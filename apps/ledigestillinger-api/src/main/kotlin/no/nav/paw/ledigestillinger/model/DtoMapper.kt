package no.nav.paw.ledigestillinger.model

import no.nav.paw.ledigestillinger.api.models.Kategori
import no.nav.paw.ledigestillinger.api.models.Lokasjon
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.model.dao.KategoriRow
import no.nav.paw.ledigestillinger.model.dao.LokasjonRow
import no.nav.paw.ledigestillinger.model.dao.StillingRow

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
    kategorier = kategorier.map { it.asDto() },
    lokasjoner = lokasjoner.map { it.asDto() },
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