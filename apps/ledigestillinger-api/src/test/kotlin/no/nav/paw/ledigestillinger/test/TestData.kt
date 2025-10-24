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
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.toMessage
import no.nav.paw.ledigestillinger.util.toLocalDateTimeString
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.LocalDateTime
import java.util.*

@Suppress("ConstPropertyName")
object TestData {
    const val fnr1: String = "01017012345"

    val uuid1_1: UUID = UUID.fromString("725f5241-583d-47d5-90f8-42bb3cd1c013")
    val uuid1_2: UUID = UUID.fromString("d62bd67a-7641-4f6a-bca5-869dd9361a04")
    val uuid2_1: UUID = UUID.fromString("ce4f105e-16d9-410f-8aee-56136a61607e")
    val uuid2_2: UUID = UUID.fromString("cc2d73a9-e0e5-4caa-8c91-aa4b8365b85e")
    val uuid2_3: UUID = UUID.fromString("f83ae2de-52d7-458b-9e25-cbf53c144c77")
    val uuid2_4: UUID = UUID.fromString("c48fff95-e047-45a0-b14f-b58075d50897")

    val message1_1: Message<UUID, Ad> = message(
        uuid = uuid1_1,
        styrkCode = "1011",
        countyCode = "55",
        municipalCode = "5501"
    )
    val message1_2: Message<UUID, Ad> = message(
        uuid = uuid1_2,
        styrkCode = "1012",
        countyCode = "56",
        municipalCode = "5601"
    )
    val message2_1: Message<UUID, Ad> = message(
        uuid = uuid2_1,
        styrkCode = "2011",
        countyCode = "57",
        municipalCode = "5701"
    )
    val message2_2: Message<UUID, Ad> = message(
        uuid = uuid2_2,
        styrkCode = "2012",
        countyCode = "57",
        municipalCode = "5702"
    )
    val message2_3: Message<UUID, Ad> = message(
        uuid = uuid2_3,
        styrkCode = "2013",
        countyCode = "57",
        municipalCode = "5703"
    )
    val message2_4: Message<UUID, Ad> = message(
        uuid = uuid2_4,
        styrkCode = "2014",
        countyCode = "57",
        municipalCode = "5704"
    )

    fun message(
        uuid: UUID = UUID.randomUUID(),
        styrkCode: String = "9999",
        municipalCode: String = "5501",
        countyCode: String = "55"
    ): Message<UUID, Ad> = record(
        uuid = uuid,
        styrkCode = styrkCode,
        municipalCode = municipalCode,
        countyCode = countyCode
    ).toMessage()

    fun record(
        uuid: UUID = UUID.randomUUID(),
        styrkCode: String = "9999",
        municipalCode: String = "5501",
        countyCode: String = "55"
    ): ConsumerRecord<UUID, Ad> = ad(
        uuid = uuid,
        styrkCode = styrkCode,
        municipalCode = municipalCode,
        countyCode = countyCode
    ).let { ConsumerRecord("teampam.stilling-ekstern-1", 0, 0L, it.first, it.second) }

    fun ad(
        uuid: UUID = UUID.randomUUID(),
        styrkCode: String = "9999",
        municipalCode: String = "5501",
        countyCode: String = "55"
    ): Pair<UUID, Ad> {
        val ad = Ad().apply {
            this.uuid = uuid.toString()
            this.adnr = "ABCD1234"
            this.title = "Test stilling"
            this.status = AdStatus.ACTIVE
            this.privacy = PrivacyChannel.SHOW_ALL
            this.source = "FINN"
            this.medium = "FINN"
            this.reference = "https://www.finn.no/stillinger/12345678"
            this.businessName = "Testbedrift"
            this.created = LocalDateTime.now().toLocalDateTimeString()
            this.updated = LocalDateTime.now().toLocalDateTimeString()
            this.published = LocalDateTime.now().toLocalDateTimeString()
            this.administration = administration()
            this.employer = company()
            this.categories = categories(
                styrkCode = styrkCode
            )
            this.classifications = classifications()
            this.locations = locations(
                municipalCode = municipalCode,
                countyCode = countyCode
            )
            this.properties = properties()
        }
        return uuid to ad
    }

    fun administration(): Administration {
        return Administration().apply {
            this.status = AdministrationStatus.DONE
            this.remarks = emptyList()
            this.comments = "Testkommentar"
            this.reportee = "Testveileder"
            this.navIdent = "NAV1234"
        }
    }

    fun company(): Company {
        return Company().apply {
            this.name = "Testbedrift"
            this.publicName = "Testbedrift"
            this.orgnr = "999999999"
            this.parentOrgnr = "999999998"
            this.orgform = "BEDR"
        }
    }

    fun categories(
        styrkCode: String = "9999"
    ): List<StyrkCategory> {
        return listOf(
            StyrkCategory().apply {
                this.styrkCode = styrkCode
                this.name = "Testyrke"
            }
        )
    }

    fun classifications(): List<Classification> {
        return listOf(
            Classification().apply {
                this.categoryType = "STYRK08"
                this.code = "9999"
                this.name = "Testyrke"
                this.score = 1.0
                this.janzzParentId = "9999"
            }
        )
    }

    fun locations(
        municipalCode: String = "5501",
        countyCode: String = "55"
    ): List<Location> {
        return listOf(
            Location().apply {
                this.address = "Storgata 1"
                this.postalCode = "9000"
                this.city = "Tromsø"
                this.municipal = "Tromsø"
                this.municipalCode = municipalCode
                this.county = "Troms"
                this.countyCode = countyCode
                this.country = "Norge"
            }
        )
    }

    fun properties(): List<Property> {
        return listOf(
            Property().apply {
                this.key = "jobtitle"
                this.value = "Bedriftsrådgiver"
            },
            Property().apply {
                this.key = "engagementtype"
                this.value = "Fast"
            },
            Property().apply {
                this.key = "extent"
                this.value = "Heltid"
            },
            Property().apply {
                this.key = "positioncount"
                this.value = "1"
            },
            Property().apply {
                this.key = "sector"
                this.value = "Privat"
            },
            Property().apply {
                this.key = "applicationdue"
                this.value = "Søknader behandles fortløpende"
            },
            Property().apply {
                this.key = "starttime"
                this.value = "2025-11-11"
            }
        )
    }
}