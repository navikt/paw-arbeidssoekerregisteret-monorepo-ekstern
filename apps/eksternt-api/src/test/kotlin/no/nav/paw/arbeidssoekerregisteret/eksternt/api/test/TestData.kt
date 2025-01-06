package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.PeriodeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Duration
import java.time.Instant
import java.util.*

object TestData {

    private const val fnrDefault = "09099912345"
    const val fnr1 = "01017012345"
    const val fnr2 = "02017012345"
    const val fnr3 = "03017012345"
    const val fnr4 = "04017012345"
    const val fnr5 = "05017012345"
    const val fnr6 = "06017012345"
    const val fnr7 = "07017012345"
    const val fnr8 = "08017012345"
    const val fnr9 = "09017012345"
    const val fnr10 = "10017012345"
    const val fnr11 = "11017012345"
    const val fnr12 = "12017012345"
    const val fnr13 = "13017012345"
    const val fnr14 = "14017012345"
    const val fnr15 = "15017012345"
    const val fnr31 = "31017012345"
    val identitetsnummer1 = Identitetsnummer(fnr1)
    val identitetsnummer2 = Identitetsnummer(fnr2)
    val identitetsnummer3 = Identitetsnummer(fnr3)
    val identitetsnummer8 = Identitetsnummer(fnr8)
    val periodeId1 = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
    val periodeId2 = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
    val periodeId3 = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489")
    val periodeId4 = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa")
    val periodeId5 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")
    val periodeId6 = UUID.fromString("91d5e8cc-0edb-4378-ba71-39465e2ebfb8")
    val periodeId7 = UUID.fromString("0bd29537-64e8-4e09-97da-886aa3a63103")
    const val navIdent1 = "NAV1001"
    const val navIdent2 = "NAV1002"
    const val navIdent3 = "NAV1003"
    val kafkaKey1 = 10001L
    val kafkaKey2 = 10002L
    val kafkaKey3 = 10003L

    fun nyStartetPeriodeRow(
        id: Long = 1L,
        identitetsnummer: String = fnrDefault,
        periodeId: UUID = UUID.randomUUID(),
        startet: Instant = Instant.now().minus(Duration.ofDays(30)),
    ) = PeriodeRow(
        id = id,
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        startet = startet,
    )

    fun nyAvsluttetPeriodeRow(
        id: Long = 1L,
        identitetsnummer: String = fnrDefault,
        periodeId: UUID = UUID.randomUUID(),
        startet: Instant = Instant.now().minus(Duration.ofDays(30)),
        avsluttet: Instant = Instant.now()
    ) = PeriodeRow(
        id = id,
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        startet = startet,
        avsluttet = avsluttet
    )

    fun nyStartetPeriode(
        periodeId: UUID = UUID.randomUUID(),
        identitetsnummer: String = fnrDefault,
        startetMetadata: Metadata = nyMetadata(
            tidspunkt = Instant.now().minus(Duration.ofDays(30)),
            utfoertAv = nyBruker(brukerId = identitetsnummer)
        ),
        avsluttetMetadata: Metadata? = null
    ) = Periode(
        periodeId,
        identitetsnummer,
        startetMetadata,
        avsluttetMetadata
    )

    fun nyAvsluttetPeriode(
        identitetsnummer: String = fnrDefault,
        periodeId: UUID = UUID.randomUUID(),
        startetMetadata: Metadata = nyMetadata(
            tidspunkt = Instant.now().minus(Duration.ofDays(30)),
            utfoertAv = nyBruker(brukerId = identitetsnummer)
        ),
        avsluttetMetadata: Metadata = nyMetadata(
            tidspunkt = Instant.now(),
            utfoertAv = nyBruker(
                type = BrukerType.SYSTEM,
                brukerId = "ARENA"
            )
        )
    ) = Periode(
        periodeId,
        identitetsnummer,
        startetMetadata,
        avsluttetMetadata
    )

    fun nyPeriodeList(size: Int = 1, identitetsnummer: String = fnrDefault) =
        IntRange(1, size).map { i ->
            val startetMetadata = nyMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30 + i.toLong())))
            nyStartetPeriode(identitetsnummer = identitetsnummer, startetMetadata = startetMetadata)
        }

    fun nyMetadata(
        tidspunkt: Instant = Instant.now(),
        utfoertAv: Bruker = nyBruker(),
        kilde: String = "KILDE",
        aarsak: String = "AARSAK",
        tidspunktFraKilde: TidspunktFraKilde = nyTidspunktFraKilde()
    ) = Metadata(
        tidspunkt,
        utfoertAv,
        kilde,
        aarsak,
        tidspunktFraKilde
    )

    fun nyBruker(
        type: BrukerType = BrukerType.SLUTTBRUKER,
        brukerId: String = fnrDefault
    ) = Bruker(type, brukerId)

    fun nyTidspunktFraKilde(
        tidspunkt: Instant = Instant.now(),
        avviksType: AvviksType = AvviksType.UKJENT_VERDI
    ) = TidspunktFraKilde(tidspunkt, avviksType)

    fun nyConsumerRecord(
        topic: String,
        partition: Int = 0,
        offset: Long = 0,
        key: Long = 1,
        value: Periode = nyStartetPeriode()
    ) = ConsumerRecord(topic, partition, offset, key, value)
}
