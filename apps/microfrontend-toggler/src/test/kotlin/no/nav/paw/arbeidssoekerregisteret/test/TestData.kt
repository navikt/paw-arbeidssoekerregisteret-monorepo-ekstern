package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.felles.model.AktorId
import no.nav.paw.felles.model.ArbeidssoekerId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.felles.model.RecordKey
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import java.time.Duration
import java.time.Instant
import java.util.*

val periodeFactory = PeriodeFactory.create()
val metadataFactory = MetadataFactory.create()
val brukerFactory = BrukerFactory.create()

object TestData {

    val identitetsnummer1 = Identitetsnummer("01017012345")
    val identitetsnummer2 = Identitetsnummer("02017012345")
    val identitetsnummer3 = Identitetsnummer("03017012345")
    val identitetsnummer4 = Identitetsnummer("04017012345")
    val identitetsnummer5 = Identitetsnummer("05017012345")
    val arbeidsoekerId1 = ArbeidssoekerId(10001L)
    val arbeidsoekerId2 = ArbeidssoekerId(10002L)
    val arbeidsoekerId3 = ArbeidssoekerId(10003L)
    val arbeidsoekerId4 = ArbeidssoekerId(10004L)
    val arbeidsoekerId5 = ArbeidssoekerId(10005L)
    val aktorId1 = AktorId("1000000001")
    val aktorId2 = AktorId("1000000002")
    val aktorId3 = AktorId("1000000003")
    val aktorId4 = AktorId("1000000004")
    val aktorId5 = AktorId("1000000005")
    val navIdent1 = NavIdent("NAV1001")
    val navIdent2 = NavIdent("NAV1002")
    val navIdent3 = NavIdent("NAV1003")
    val navIdent4 = NavIdent("NAV1004")
    val navIdent5 = NavIdent("NAV1005")
    val kafkaKey1 = RecordKey(-10001L)
    val kafkaKey2 = RecordKey(-10002L)
    val kafkaKey3 = RecordKey(-10003L)
    val kafkaKey4 = RecordKey(-10004L)
    val kafkaKey5 = RecordKey(-10005L)
    val periodeId1 = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
    val periodeId2 = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
    val periodeId3 = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489")
    val periodeId4 = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa")
    val periodeId5 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")

    val kafkaKeysResponse1 = KafkaKeysResponse(arbeidsoekerId1.value, kafkaKey1.value)
    val kafkaKeysResponse2 = KafkaKeysResponse(arbeidsoekerId2.value, kafkaKey2.value)
    val kafkaKeysResponse3 = KafkaKeysResponse(arbeidsoekerId3.value, kafkaKey3.value)
    val kafkaKeysResponse4 = KafkaKeysResponse(arbeidsoekerId4.value, kafkaKey4.value)
    val kafkaKeysResponse5 = KafkaKeysResponse(arbeidsoekerId5.value, kafkaKey5.value)

    val periode1Startet = buildPeriode(
        id = periodeId1,
        identitetsnummer = identitetsnummer1,
        startetTidspunkt = Instant.now().minus(Duration.ofDays(27))
    )
    val periode1Avsluttet = periode1Startet.avslutt(
        avsluttetTidspunkt = Instant.now().minus(Duration.ofDays(22))
    )
    val periode2Startet = buildPeriode(
        id = periodeId2,
        identitetsnummer = identitetsnummer2,
        startetTidspunkt = Instant.now().minus(Duration.ofDays(10))
    )
    val periode2Avsluttet = periode2Startet.avslutt(
        avsluttetTidspunkt = Instant.now().minus(Duration.ofDays(5))
    )
    val periode3Startet1 = buildPeriode(
        id = periodeId3,
        identitetsnummer = identitetsnummer3,
        startetTidspunkt = Instant.now().minus(Duration.ofDays(30))
    )
    val periode3Startet2 = buildPeriode(
        id = periodeId3,
        identitetsnummer = identitetsnummer3,
        startetTidspunkt = Instant.now().minus(Duration.ofDays(60))
    )
    val periode4Startet = buildPeriode(
        id = periodeId1,
        identitetsnummer = identitetsnummer4,
        startetTidspunkt = Instant.now().minus(Duration.ofDays(10))
    )
    val periode4Avsluttet = periode4Startet.avslutt(
        avsluttetTidspunkt = Instant.now()
    )

    fun buildPeriode(
        id: UUID = UUID.randomUUID(),
        identitetsnummer: Identitetsnummer = identitetsnummer1,
        startetTidspunkt: Instant = Instant.now(),
        avsluttetTidspunkt: Instant? = null
    ): Periode = periodeFactory.build(
        id = id,
        identitetsnummer = identitetsnummer.value,
        startet = metadataFactory.build(
            tidspunkt = startetTidspunkt,
            utfortAv = brukerFactory.build(id = identitetsnummer.value)
        ),
        avsluttetTidspunkt?.let {
            metadataFactory.build(
                tidspunkt = it,
                utfortAv = brukerFactory.build(id = identitetsnummer.value)
            )
        }
    )
}

fun Periode.avslutt(avsluttetTidspunkt: Instant): Periode = Periode(
    this.id,
    this.identitetsnummer,
    this.startet,
    metadataFactory.build(
        tidspunkt = avsluttetTidspunkt,
        utfortAv = brukerFactory.build(id = this.identitetsnummer)
    )
)
