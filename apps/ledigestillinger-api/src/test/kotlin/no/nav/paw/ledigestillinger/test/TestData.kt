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
import no.nav.paw.ledigestillinger.util.toLocalDateTimeString
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
        it.created = LocalDateTime.now().toLocalDateTimeString()
        it.updated = LocalDateTime.now().toLocalDateTimeString()
        it.published = LocalDateTime.now().toLocalDateTimeString()
        it.administration = administration()
        it.employer = company()
        it.categories = categories()
        it.classifications = classifications()
        it.locations = locations()
        it.properties = properties()
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

    fun categories(): List<StyrkCategory> {
        return listOf(
            StyrkCategory().apply {
                styrkCode = "9999"
                name = "Testyrke"
            }
        )
    }

    fun classifications(): List<Classification> {
        return listOf(
            Classification().apply {
                categoryType = "STYRK08"
                code = "9999"
                name = "Testyrke"
                score = 1.0
                janzzParentId = "9999"
            }
        )
    }

    fun locations(): List<Location> {
        return listOf(
            Location().apply {
                address = "Storgata 1"
                postalCode = "9000"
                city = "Tromsø"
                municipal = "Tromsø"
                municipalCode = "5501"
                county = "Troms"
                countyCode = "55"
                country = "Norge"
            }
        )
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