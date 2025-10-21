package no.nav.paw.ledigestillinger.test

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.pam.stilling.ext.avro.Administration
import no.nav.pam.stilling.ext.avro.AdministrationStatus
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.PrivacyChannel
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.paw.ledigestillinger.util.toIsoString
import java.time.LocalDateTime
import java.util.*

object TestData {
    fun ad(): Pair<UUID, Ad> {
        val uuid = UUID.randomUUID()
        val it = Ad()
        it.uuid = uuid.toString()
        it.adnr = "ABCD1234"
        it.title = "Test stilling"
        it.status = AdStatus.ACTIVE
        it.privacy = PrivacyChannel.SHOW_ALL
        it.source = "FINN"
        it.medium = "FINN"
        it.reference = "https://www.finn.no/stillinger/12345678"
        it.businessName = "Testbedrift"
        it.created = LocalDateTime.now().toIsoString()
        it.updated = LocalDateTime.now().toIsoString()
        it.published = LocalDateTime.now().toIsoString()
        it.administration = administration()
        it.employer = company()
        it.categories = listOf(category())
        it.classifications = listOf(classification())
        it.locations = listOf(location())
        it.properties = properties()
        it.classifications = listOf(classification())
        return uuid to it
    }

    fun administration(): Administration {
        val it = Administration()
        it.status = AdministrationStatus.DONE
        it.remarks = emptyList()
        it.comments = "Testkommentar"
        it.reportee = "Testveileder"
        it.navIdent = "NAV1234"
        return it
    }

    fun company(): Company {
        val it = Company()
        it.name = "Testbedrift"
        it.publicName = "Testbedrift"
        it.orgnr = "999999999"
        it.parentOrgnr = "999999998"
        it.orgform = "BEDR"
        return it
    }

    fun category(): StyrkCategory {
        val it = StyrkCategory()
        it.styrkCode = "9999"
        it.name = "Testyrke"
        return it
    }

    fun classification(): Classification {
        val it = Classification()
        it.categoryType = "STYRK08"
        it.code = "9999"
        it.name = "Testyrke"
        it.score = 1.0
        it.janzzParentId = "9999"
        return it
    }

    fun location(): Location {
        val it = Location()
        it.address = "Storgata 1"
        it.postalCode = "9000"
        it.city = "Tromsø"
        it.municipal = "Tromsø"
        it.municipalCode = "5501"
        it.county = "Troms"
        it.countyCode = "55"
        it.country = "Norge"
        return it
    }

    fun properties(): List<Property> {
        return listOf(
            Property().apply {
                key = "extent"
                value = "Heltid"
            },
            Property().apply {
                key = "jobtitle"
                value = "Bedriftsrådgiver"
            },
            Property().apply {
                key = "applicationdue"
                value = "Søknader behandles fortløpende"
            },
            Property().apply {
                key = "engagementtype"
                value = "Fast"
            },
            Property().apply {
                key = "sector"
                value = "Privat"
            }
        )
    }
}