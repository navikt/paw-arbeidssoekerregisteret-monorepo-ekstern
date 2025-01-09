package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.common.types.identer.AktorId
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import java.time.Instant
import java.util.*

val periodeFactory = PeriodeFactory.create()
val metadataFactory = MetadataFactory.create()
val brukerFactory = BrukerFactory.create()

object TestData {

    const val fnr1 = "01017012345"
    const val fnr2 = "02017012345"
    const val fnr3 = "03017012345"
    const val fnr4 = "04017012345"
    const val fnr5 = "05017012345"
    val identitetsnummer1 = Identitetsnummer(fnr1)
    val identitetsnummer2 = Identitetsnummer(fnr2)
    val identitetsnummer3 = Identitetsnummer(fnr3)
    val identitetsnummer4 = Identitetsnummer(fnr4)
    val identitetsnummer5 = Identitetsnummer(fnr5)
    val arbeidsoekerId1 = 10001L
    val arbeidsoekerId2 = 10002L
    val arbeidsoekerId3 = 10003L
    val arbeidsoekerId4 = 10004L
    val arbeidsoekerId5 = 10005L
    const val aktorId1 = "1000000001"
    const val aktorId2 = "1000000002"
    const val aktorId3 = "1000000003"
    const val aktorId4 = "1000000004"
    const val aktorId5 = "1000000005"
    const val navIdent1 = "NAV1001"
    const val navIdent2 = "NAV1002"
    const val navIdent3 = "NAV1003"
    const val navIdent4 = "NAV1004"
    const val navIdent5 = "NAV1005"
    val kafkaKey1 = -10001L
    val kafkaKey2 = -10002L
    val kafkaKey3 = -10003L
    val kafkaKey4 = -10004L
    val kafkaKey5 = -10005L
    val periodeId1 = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
    val periodeId2 = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
    val periodeId3 = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489")
    val periodeId4 = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa")
    val periodeId5 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")

    fun buildPeriode(
        id: UUID = UUID.randomUUID(),
        identitetsnummer: Identitetsnummer = identitetsnummer1,
        startetTidspunkt: Instant = Instant.now(),
        avsluttetTidspunkt: Instant? = null
    ) = periodeFactory.build(
        id = id,
        identitetsnummer = identitetsnummer.verdi,
        startet = metadataFactory.build(
            tidspunkt = startetTidspunkt,
            utfortAv = brukerFactory.build(id = identitetsnummer.verdi)
        ),
        avsluttetTidspunkt?.let {
            metadataFactory.build(
                tidspunkt = it,
                utfortAv = brukerFactory.build(id = identitetsnummer.verdi)
            )
        }
    )

    fun buildSiste14aVedtak(
        aktorId: String = aktorId1,
        fattetDato: Instant = Instant.now()
    ) = Siste14aVedtak(
        AktorId(aktorId),
        "STANDARD_INNSATS",
        "SKAFFE_ARBEID",
        fattetDato,
        false
    )

    fun buildBeriket14aVedtak(
        aktorId: String = aktorId1,
        arbeidsoekerId: Long = arbeidsoekerId1,
        fattetDato: Instant = Instant.now()
    ) = Beriket14aVedtak(
        aktorId,
        arbeidsoekerId,
        "STANDARD_INNSATS",
        "SKAFFE_ARBEID",
        fattetDato,
        false
    )
}
