package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.DetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningRow
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import java.time.Duration
import java.time.Instant
import java.util.*
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker as BekreftelseBruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType as BekreftelseBrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata as BekreftelseMetadata

object TestData {

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
    val identitetsnummer1 = Identitetsnummer(fnr1)
    val identitetsnummer2 = Identitetsnummer(fnr2)
    val identitetsnummer3 = Identitetsnummer(fnr3)
    val identitetsnummer8 = Identitetsnummer(fnr8)
    val periodeId1 = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
    val periodeId2 = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
    val periodeId3 = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489")
    val opplysningerId1 = UUID.fromString("d4086937-2d27-4c87-ad6d-81deb76f50d3")
    val opplysningerId2 = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa")
    val opplysningerId3 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")
    val profileringId1 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")
    val profileringId2 = UUID.fromString("91d5e8cc-0edb-4378-ba71-39465e2ebfb8")
    val profileringId3 = UUID.fromString("0bd29537-64e8-4e09-97da-886aa3a63103")
    const val navIdent1 = "NAV1001"
    const val navIdent2 = "NAV1002"
    const val navIdent3 = "NAV1003"
    val kafkaKey1 = fnr1.toLong()
    val kafkaKey2 = fnr2.toLong()
    val kafkaKey3 = fnr3.toLong()

    fun nyNavAnsatt(
        azureId: String = UUID.randomUUID().toString(),
        navIdent: String = navIdent1
    ) = NavAnsatt(azureId = azureId, navIdent = navIdent)

    fun nyOpplysningerRow(
        id: Long = 1L,
        opplysningerId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        sendtInnAv: MetadataRow = nyMetadataRow(),
        jobbsituasjon: List<BeskrivelseMedDetaljerRow> = nyJobbsituasjonRows(
            listOf(
                Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
            )
        ),
        utdanning: UtdanningRow? = nyUtdanningRow(),
        helse: HelseRow? = nyHelseRow(),
        annet: AnnetRow? = nyAnnetRow(),
    ) = OpplysningerRow(
        id = id,
        opplysningerId = opplysningerId,
        periodeId = periodeId,
        sendtInnAv = sendtInnAv,
        jobbsituasjon = jobbsituasjon,
        utdanning = utdanning,
        helse = helse,
        annet = annet
    )

    fun nyJobbsituasjonRows(
        beskrivelser: List<Beskrivelse> = listOf(Beskrivelse.HAR_SAGT_OPP)
    ): List<BeskrivelseMedDetaljerRow> =
        beskrivelser.map { nyBeskrivelseMedDetaljerRow(beskrivelse = it) }

    fun nyBeskrivelseMedDetaljerRow(
        id: Long = 1L,
        beskrivelse: Beskrivelse = Beskrivelse.HAR_SAGT_OPP,
        detaljer: List<DetaljerRow> = listOf(nyDetaljerRow()),
    ): BeskrivelseMedDetaljerRow = BeskrivelseMedDetaljerRow(
        id = id,
        beskrivelse = beskrivelse,
        detaljer = detaljer
    )

    fun nyDetaljerRow(
        id: Long = 1L,
        noekkel: String = "NOEKKEL",
        verdi: String = "VERDI"
    ) = DetaljerRow(
        id = id,
        noekkel = noekkel,
        verdi = verdi
    )

    fun nyUtdanningRow(
        id: Long = 1L,
        nus: String = "NUS_KODE",
        bestaatt: JaNeiVetIkke? = JaNeiVetIkke.JA,
        godkjent: JaNeiVetIkke? = JaNeiVetIkke.JA
    ) = UtdanningRow(
        id = id,
        nus = nus,
        bestaatt = bestaatt,
        godkjent = godkjent
    )

    fun nyHelseRow(
        id: Long = 1L,
        helsetilstandHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI
    ) = HelseRow(
        id = id,
        helsetilstandHindrerArbeid = helsetilstandHindrerArbeid
    )

    fun nyAnnetRow(
        id: Long = 1L,
        andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.NEI
    ) = AnnetRow(
        id = id,
        andreForholdHindrerArbeid = andreForholdHindrerArbeid
    )

    fun nyStartetPeriodeRow(
        id: Long = 1L,
        identitetsnummer: String = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        startetMetadata: MetadataRow = nyMetadataRow(
            tidspunkt = Instant.now().minus(Duration.ofDays(30)),
            utfoertAv = nyBrukerRow(brukerId = identitetsnummer)
        ),
        avsluttetMetadata: MetadataRow? = null
    ) = PeriodeRow(
        id = id,
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        startet = startetMetadata,
        avsluttet = avsluttetMetadata
    )

    fun nyAvsluttetPeriodeRow(
        id: Long = 1L,
        identitetsnummer: String = fnr1,
        periodeId: UUID = UUID.randomUUID(),
        startetMetadata: MetadataRow = nyMetadataRow(
            tidspunkt = Instant.now().minus(Duration.ofDays(30)),
            utfoertAv = nyBrukerRow(brukerId = identitetsnummer)
        ),
        avsluttetMetadata: MetadataRow = nyMetadataRow(
            tidspunkt = Instant.now(),
            utfoertAv = nyBrukerRow(
                type = no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SYSTEM,
                brukerId = "ARENA"
            )
        )
    ) = PeriodeRow(
        id = id,
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        startet = startetMetadata,
        avsluttet = avsluttetMetadata
    )

    fun nyProfileringRow(
        id: Long = 1L,
        profileringId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        opplysningerId: UUID = opplysningerId1,
        sendtInAv: MetadataRow = nyMetadataRow(),
        profilertTil: ProfilertTil = ProfilertTil.UDEFINERT,
        jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean = true,
        alder: Int = 30
    ) = ProfileringRow(
        id = id,
        profileringId = profileringId,
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerId,
        sendtInAv,
        profilertTil = profilertTil,
        jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder = alder
    )

    fun nyMetadataRow(
        id: Long = 1L,
        tidspunkt: Instant = Instant.now(),
        utfoertAv: BrukerRow = nyBrukerRow(),
        kilde: String = "KILDE",
        aarsak: String = "AARSAK",
        tidspunktFraKilde: TidspunktFraKildeRow = nyTidspunktFraKildeRow()
    ) = MetadataRow(
        id = id,
        tidspunkt = tidspunkt,
        utfoertAv = utfoertAv,
        kilde = kilde,
        aarsak = aarsak,
        tidspunktFraKilde = tidspunktFraKilde
    )

    fun nyBrukerRow(
        id: Long = 1L,
        type: no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType = no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER,
        brukerId: String = fnr1
    ) = BrukerRow(id = id, type = type, brukerId = brukerId)

    fun nyTidspunktFraKildeRow(
        id: Long = 1L,
        tidspunkt: Instant = Instant.now(),
        avviksType: AvviksType = AvviksType.UKJENT_VERDI
    ) = TidspunktFraKildeRow(id = id, tidspunkt = tidspunkt, avviksType = avviksType)

    fun nyOpplysningerRowList(periodeId: UUID = periodeId1): List<OpplysningerRow> = listOf(
        nyOpplysningerRow(opplysningerId = opplysningerId1, periodeId = periodeId),
        nyOpplysningerRow(opplysningerId = opplysningerId2, periodeId = periodeId),
        nyOpplysningerRow(opplysningerId = opplysningerId3, periodeId = periodeId)
    )

    fun nyPeriodeRowList(): List<PeriodeRow> = listOf(
        nyStartetPeriodeRow(periodeId = periodeId1),
        nyAvsluttetPeriodeRow(periodeId = periodeId2),
        nyAvsluttetPeriodeRow(periodeId = periodeId3),
    )

    fun nyProfileringRowList(size: Int = 1, periodeId: UUID = periodeId1): List<ProfileringRow> =
        IntRange(1, size).map {
            nyProfileringRow(
                profileringId = profileringId1,
                periodeId = periodeId,
                opplysningerId = opplysningerId1
            )
        }

    fun nyStartetPeriode(
        periodeId: UUID = UUID.randomUUID(),
        identitetsnummer: String = fnr1,
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
        identitetsnummer: String = fnr1,
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

    fun nyPeriodeList(size: Int = 1, identitetsnummer: String = fnr1) =
        IntRange(1, size).map { nyStartetPeriode(identitetsnummer = identitetsnummer) }

    fun nyOpplysningerOmArbeidssoeker(
        opplysningerId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        sendtInnAv: Metadata = nyMetadata(),
        jobbsituasjon: Jobbsituasjon = nyJobbsituasjon(
            listOf(
                Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
            )
        ),
        utdanning: Utdanning? = nyUtdanning(),
        helse: Helse? = nyHelse(),
        annet: Annet? = nyAnnet(),
    ) = OpplysningerOmArbeidssoeker(
        opplysningerId,
        periodeId,
        sendtInnAv,
        utdanning,
        helse,
        jobbsituasjon,
        annet
    )

    fun nyOpplysningerOmArbeidssoekerList(size: Int = 1, periodeId: UUID = UUID.randomUUID()) =
        IntRange(1, size).map { nyOpplysningerOmArbeidssoeker(periodeId = periodeId) }

    fun nyJobbsituasjon(
        beskrivelser: List<Beskrivelse> = listOf(Beskrivelse.HAR_SAGT_OPP)
    ): Jobbsituasjon {
        val beskrivelseMedDetaljer = beskrivelser.map { nyBeskrivelseMedDetaljer(beskrivelse = it) }
        return Jobbsituasjon(beskrivelseMedDetaljer)
    }

    fun nyBeskrivelseMedDetaljer(
        beskrivelse: Beskrivelse = Beskrivelse.HAR_SAGT_OPP,
        detaljer: Map<String, String> = mapOf("NOEKKEL" to "VERDI"),
    ) = BeskrivelseMedDetaljer(
        beskrivelse,
        detaljer
    )

    fun nyUtdanning(
        nus: String = "NUS_KODE",
        bestaatt: JaNeiVetIkke? = JaNeiVetIkke.JA,
        godkjent: JaNeiVetIkke? = JaNeiVetIkke.JA
    ) = Utdanning(nus, bestaatt, godkjent)

    fun nyHelse(
        helsetilstandHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI
    ) = Helse(helsetilstandHindrerArbeid)

    fun nyAnnet(
        andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.NEI
    ) = Annet(andreForholdHindrerArbeid)

    fun nyProfilering(
        profileringId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        opplysningerId: UUID = opplysningerId1,
        sendtInAv: Metadata = nyMetadata(),
        profilertTil: ProfilertTil = ProfilertTil.UDEFINERT,
        jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean = true,
        alder: Int = 30
    ) = Profilering(
        profileringId,
        periodeId,
        opplysningerId,
        sendtInAv,
        profilertTil,
        jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder
    )

    fun nyProfileringList(size: Int = 1, periodeId: UUID = periodeId1) =
        IntRange(1, size).map { nyProfilering(periodeId = periodeId) }

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
        brukerId: String = fnr1
    ) = Bruker(type, brukerId)

    fun nyTidspunktFraKilde(
        tidspunkt: Instant = Instant.now(),
        avviksType: AvviksType = AvviksType.UKJENT_VERDI
    ) = TidspunktFraKilde(tidspunkt, avviksType)

    fun nyBekreftelse(
        bekreftelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
        svar: Svar = nyBekreftelseSvar()
    ) = Bekreftelse(periodeId, bekreftelsesloesning, bekreftelseId, svar)

    fun nyBekreftelseSvar(
        sendtInn: BekreftelseMetadata = nyBekreftelseMetadata(),
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now().plus(Duration.ofDays(14)),
        harJobbetIDennePerioden: Boolean = false,
        vilFortsetteSomArbeidssoeker: Boolean = true
    ) = Svar(
        sendtInn,
        gjelderFra,
        gjelderTil,
        harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker
    )

    fun nyBekreftelseMetadata(
        tidspunkt: Instant = Instant.now(),
        utfoertAv: BekreftelseBruker = nyBekreftelseBruker(),
        kilde: String = "KILDE",
        aarsak: String = "AARSAK",
    ) = BekreftelseMetadata(tidspunkt, utfoertAv, kilde, aarsak)

    fun nyBekreftelseBruker(
        type: BekreftelseBrukerType = BekreftelseBrukerType.SLUTTBRUKER,
        brukerId: String = fnr1
    ) = BekreftelseBruker(type, brukerId)

    fun nyBekreftelseList(
        size: Int = 1,
        periodeId: UUID = periodeId1
    ) = IntRange(1, size).map { nyBekreftelse(periodeId = periodeId) }
}
