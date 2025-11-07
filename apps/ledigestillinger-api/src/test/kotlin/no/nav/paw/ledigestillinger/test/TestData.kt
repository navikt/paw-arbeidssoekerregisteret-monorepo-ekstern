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
import no.naw.paw.ledigestillinger.model.KlassifiseringType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.*

@Suppress("ConstPropertyName")
object TestData {
    const val fnr1: String = "01017012345"

    val uuid1_1: UUID = UUID.fromString("725f5241-583d-47d5-90f8-42bb3cd1c013")
    val uuid1_2: UUID = UUID.fromString("d62bd67a-7641-4f6a-bca5-869dd9361a04")
    val uuid2_1: UUID = UUID.fromString("e43a888e-1ffc-48d2-805b-c77080cd3913")
    val uuid2_2: UUID = UUID.fromString("cc2d73a9-e0e5-4caa-8c91-aa4b8365b85e")
    val uuid3_1: UUID = UUID.fromString("53df2194-5597-4ae0-a773-cd25b6056d3f")
    val uuid3_2: UUID = UUID.fromString("5d7774af-8b39-4607-9db4-bdd731478c14")
    val uuid4_1: UUID = UUID.fromString("ce4f105e-16d9-410f-8aee-56136a61607e")
    val uuid4_2: UUID = UUID.fromString("590c18b4-a0e1-40f3-afd6-0709d9cb9c2c")
    val uuid5_1: UUID = UUID.fromString("0651bb04-4f3f-421c-85e5-33ebdb1ebddf")
    val uuid5_2: UUID = UUID.fromString("b54bd99b-8a64-4098-aaa2-b6c3453f4b0e")
    val uuid5_3: UUID = UUID.fromString("370ab569-1509-46a0-b442-83d085b41869")
    val uuid5_4: UUID = UUID.fromString("b14b0b91-57af-4e19-ab06-21626b03043f")
    val uuid5_5: UUID = UUID.fromString("3957b323-0202-43e6-a959-eb421a50a598")
    val adnr1_1: String = "ABCD1011"
    val adnr1_2: String = "ABCD1012"
    val adnr2_1: String = "ABCD2011"
    val adnr2_2: String = "ABCD2012"
    val adnr3_1: String = "ABCD3011"
    val adnr3_2: String = "ABCD3012"
    val adnr4_1: String = "ABCD4011"
    val adnr4_2: String = "ABCD4012"
    val adnr5_1: String = "ABCD5011"
    val adnr5_2: String = "ABCD5012"
    val adnr5_3: String = "ABCD5013"
    val adnr5_4: String = "ABCD5014"
    val adnr5_5: String = "ABCD5015"

    val message1_1: Message<UUID, Ad> = message(
        uuid = uuid1_1,
        adnr = adnr1_1,
        published = LocalDateTime.now().minusDays(8),
        categories = categories(styrkCode = "1011"),
        locations = locations(municipalCode = "1011", countyCode = "10")
    )
    val message1_2: Message<UUID, Ad> = message(
        uuid = uuid1_2,
        adnr = adnr1_2,
        published = LocalDateTime.now().minusDays(7),
        categories = categories(styrkCode = "1012"),
        locations = locations(municipalCode = "1012", countyCode = "10")
    )
    val message2_1: Message<UUID, Ad> = message(
        uuid = uuid2_1,
        adnr = adnr2_1,
        published = LocalDateTime.now().minusDays(6),
        categories = categories(styrkCode = "2011"),
        locations = locations(municipalCode = "2011", countyCode = "20")
    )
    val message2_2: Message<UUID, Ad> = message(
        uuid = uuid2_2,
        adnr = adnr2_2,
        published = LocalDateTime.now().minusDays(5),
        categories = categories(styrkCode = "2012"),
        locations = locations(municipalCode = "2012", countyCode = "20")
    )
    val message3_1: Message<UUID, Ad> = message(
        uuid = uuid3_1,
        adnr = adnr3_1,
        published = LocalDateTime.now().minusDays(4),
        categories = categories(styrkCode = "3011"),
        locations = locations(municipalCode = "3011", countyCode = "30")
    )
    val message3_2: Message<UUID, Ad> = message(
        uuid = uuid3_2,
        adnr = adnr3_2,
        published = LocalDateTime.now().minusDays(3),
        categories = categories(styrkCode = "3012"),
        locations = locations(municipalCode = "3012", countyCode = "30")
    )
    val message4_1: Message<UUID, Ad> = message(
        uuid = uuid4_1,
        adnr = adnr4_1,
        published = LocalDateTime.now().minusDays(2),
        classifications = classifications(categoryType = KlassifiseringType.STYRK08, code = "4011"),
        locations = locations(municipalCode = "4011", countyCode = "40")
    )
    val message4_2: Message<UUID, Ad> = message(
        uuid = uuid4_2,
        adnr = adnr4_2,
        published = LocalDateTime.now().minusDays(1),
        categories = categories(styrkCode = "4012"),
        locations = locations(municipalCode = "4012", countyCode = "40")
    )
    val message5_1: Message<UUID, Ad> = message(
        uuid = uuid5_1,
        adnr = adnr5_1,
        source = "AMEDIA",
        privacy = PrivacyChannel.SHOW_ALL,
        published = LocalDateTime.now().minusDays(5),
        classifications = classifications(categoryType = KlassifiseringType.STYRK08, code = "5011"),
        locations = locations(municipalCode = "5011", countyCode = "50")
    )
    val message5_2: Message<UUID, Ad> = message(
        uuid = uuid5_2,
        adnr = adnr5_2,
        source = "AMEDIA",
        privacy = PrivacyChannel.INTERNAL_NOT_SHOWN,
        published = LocalDateTime.now().minusDays(4),
        categories = categories(styrkCode = "5012"),
        locations = locations(municipalCode = "5012", countyCode = "50")
    )
    val message5_3: Message<UUID, Ad> = message(
        uuid = uuid5_3,
        adnr = adnr5_3,
        source = "IMPORTAPI",
        privacy = PrivacyChannel.SHOW_ALL,
        published = LocalDateTime.now().minusDays(3),
        categories = categories(styrkCode = "5013"),
        locations = locations(municipalCode = "5013", countyCode = "50")
    )
    val message5_4: Message<UUID, Ad> = message(
        uuid = uuid5_4,
        adnr = adnr5_4,
        source = "DIR",
        privacy = PrivacyChannel.SHOW_ALL,
        published = LocalDateTime.now().minusDays(2),
        categories = categories(styrkCode = "5014"),
        locations = locations(municipalCode = "5014", countyCode = "50")
    )
    val message5_5: Message<UUID, Ad> = message(
        uuid = uuid5_5,
        adnr = adnr5_5,
        source = "DIR",
        privacy = PrivacyChannel.INTERNAL_NOT_SHOWN,
        published = LocalDateTime.now().minusDays(1),
        categories = categories(styrkCode = "5015"),
        locations = locations(municipalCode = "5015", countyCode = "50")
    )

    fun message(
        uuid: UUID = UUID.randomUUID(),
        adnr: String = "ABCD1234",
        status: AdStatus = AdStatus.ACTIVE,
        privacy: PrivacyChannel = PrivacyChannel.SHOW_ALL,
        source: String = "FINN",
        published: LocalDateTime = LocalDateTime.now(),
        expires: LocalDateTime? = null,
        categories: List<StyrkCategory> = emptyList(),
        classifications: List<Classification> = emptyList(),
        locations: List<Location> = emptyList()
    ): Message<UUID, Ad> = record(
        uuid = uuid,
        adnr = adnr,
        status = status,
        privacy = privacy,
        source = source,
        published = published,
        expires = expires,
        categories = categories,
        classifications = classifications,
        locations = locations
    ).toMessage()

    fun record(
        uuid: UUID = UUID.randomUUID(),
        adnr: String = "ABCD1234",
        status: AdStatus = AdStatus.ACTIVE,
        privacy: PrivacyChannel = PrivacyChannel.SHOW_ALL,
        source: String = "FINN",
        published: LocalDateTime = LocalDateTime.now(),
        expires: LocalDateTime? = null,
        categories: List<StyrkCategory> = emptyList(),
        classifications: List<Classification> = emptyList(),
        locations: List<Location> = emptyList(),
        topic: String = "teampam.stilling-ekstern-1"
    ): ConsumerRecord<UUID, Ad> = ad(
        uuid = uuid,
        adnr = adnr,
        status = status,
        privacy = privacy,
        source = source,
        published = published,
        expires = expires,
        categories = categories,
        classifications = classifications,
        locations = locations
    ).let { ConsumerRecord(topic, 0, 0L, it.first, it.second) }

    fun ad(
        uuid: UUID = UUID.randomUUID(),
        adnr: String = "ABCD1234",
        status: AdStatus = AdStatus.ACTIVE,
        privacy: PrivacyChannel = PrivacyChannel.SHOW_ALL,
        source: String = "FINN",
        published: LocalDateTime = LocalDateTime.now(),
        expires: LocalDateTime? = null,
        categories: List<StyrkCategory> = emptyList(),
        classifications: List<Classification> = emptyList(),
        locations: List<Location> = emptyList()
    ): Pair<UUID, Ad> {
        val ad = Ad().apply {
            this.uuid = uuid.toString()
            this.adnr = adnr
            this.title = "Test stilling"
            this.status = status
            this.privacy = privacy
            this.source = source
            this.medium = "FINN"
            this.reference = "https://www.finn.no/stillinger/12345678"
            this.businessName = "Testbedrift"
            this.created = LocalDateTime.now().toLocalDateTimeString()
            this.updated = LocalDateTime.now().toLocalDateTimeString()
            this.published = published.toLocalDateTimeString()
            this.expires = expires?.toLocalDateTimeString()
            this.administration = administration()
            this.employer = company()
            this.categories = categories
            this.classifications = classifications
            this.locations = locations
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

    fun classifications(
        categoryType: KlassifiseringType = KlassifiseringType.STYRK08,
        code: String = "9999"
    ): List<Classification> {
        return listOf(
            Classification().apply {
                this.categoryType = categoryType.name
                this.code = code
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

fun <K, V> Message<K, V>.asProducerRecord(): ProducerRecord<K, V> = ProducerRecord(topic, key, value)