package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

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
import java.time.Duration
import java.time.Instant
import java.util.*

const val fnr1 = "01017012345"
const val fnr2 = "02017012345"
const val fnr3 = "03017012345"
val periodeId1 = UUID.fromString("6d6302a7-7ed1-40a3-8257-c3e8ade4c049")
val periodeId2 = UUID.fromString("2656398c-a355-4f9b-8b34-a76abaf3c61a")
val periodeId3 = UUID.fromString("44b44747-f65d-46b3-89a8-997a63d0d489")
val opplysningerId1 = UUID.fromString("d4086937-2d27-4c87-ad6d-81deb76f50d3")
val opplysningerId2 = UUID.fromString("f0e09ebf-e9f7-4025-9bd7-31bbff037eaa")
val opplysningerId3 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")
val profileringId1 = UUID.fromString("e7b8c9f6-9ada-457c-bed5-ec45656c73b2")
val profileringId2 = UUID.fromString("91d5e8cc-0edb-4378-ba71-39465e2ebfb8")
val profileringId3 = UUID.fromString("0bd29537-64e8-4e09-97da-886aa3a63103")
const val navIdent1 = "NO12345"
const val navIdent2 = "NO23456"
const val navIdent3 = "NO34567"

fun nyOpplysning(
    opplysningerId: UUID = UUID.randomUUID(),
    periodeId: UUID = periodeId1,
    sendtInAv: Metadata = nyMetadata(bruker = nyBruker(id = fnr1)),
    utdanning: Utdanning? = nyUtdanning(),
    helse: Helse? = nyHelse(),
    jobbsituasjon: Jobbsituasjon? = nyJobbsituasjon(
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
    ),
    annet: Annet? = nyAnnet(),
) = OpplysningerOmArbeidssoeker(
    opplysningerId,
    periodeId,
    sendtInAv,
    utdanning,
    helse,
    jobbsituasjon,
    annet
)

fun nyUtdanning(
    nus: String = "NUS_KODE",
    bestaat: JaNeiVetIkke? = JaNeiVetIkke.VET_IKKE,
    godkjent: JaNeiVetIkke? = JaNeiVetIkke.VET_IKKE
) = Utdanning(nus, bestaat, godkjent)

fun nyHelse(
    helsetilstandHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.VET_IKKE
) = Helse(helsetilstandHindrerArbeid)

fun nyAnnet(
    andreForholdHindrerArbeid: JaNeiVetIkke? = JaNeiVetIkke.VET_IKKE
) = Annet(andreForholdHindrerArbeid)

fun nyJobbsituasjon(vararg besktivelser: Beskrivelse): Jobbsituasjon {
    val beskrivelseMedDetaljer = besktivelser.map {
        BeskrivelseMedDetaljer(
            it, mapOf(
                Pair("noekkel1", "verdi1"),
                Pair("noekkel2", "verdi2")
            )
        )
    }.toList()
    return Jobbsituasjon(beskrivelseMedDetaljer)
}

fun nyStartetPeriode(
    identitetsnummer: String = fnr1,
    periodeId: UUID = UUID.randomUUID(),
    startetMetadata: Metadata = nyMetadata(
        tidspunkt = Instant.now().minus(Duration.ofDays(30)),
        bruker = nyBruker(id = identitetsnummer)
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
        bruker = nyBruker(id = identitetsnummer)
    ),
    avsluttetMetadata: Metadata = nyMetadata(
        tidspunkt = Instant.now(),
        bruker = nyBruker(type = BrukerType.SYSTEM, id = "ARENA")
    )
) = Periode(
    periodeId,
    identitetsnummer,
    startetMetadata,
    avsluttetMetadata
)

fun nyProfilering(
    profileringId: UUID = UUID.randomUUID(),
    periodeId: UUID = periodeId1,
    opplysningerId: UUID = opplysningerId1,
    sendtInAv: Metadata = nyMetadata(),
    profilertTil: ProfilertTil = ProfilertTil.UDEFINERT,
    jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean = true,
    alder: Int = 30
) = Profilering(
    profileringId,
    periodeId,
    opplysningerId,
    sendtInAv,
    profilertTil,
    jobbetSammenhengendeSeksAvTolvSisteMnd,
    alder
)

fun nyMetadata(
    tidspunkt: Instant = Instant.now(),
    bruker: Bruker = nyBruker(),
    kilde: String = "KILDE",
    aarsak: String = "AARSAK",
    tidspunktFraKilde: TidspunktFraKilde = nyTidspunktFraKilde()
) = Metadata(
    tidspunkt,
    bruker,
    kilde,
    aarsak,
    tidspunktFraKilde
)

fun nyBruker(
    type: BrukerType = BrukerType.SLUTTBRUKER,
    id: String = fnr1
) = Bruker(type, id)

fun nyTidspunktFraKilde(
    tidspunkt: Instant = Instant.now(),
    avviksType: AvviksType = AvviksType.UKJENT_VERDI
) = TidspunktFraKilde(tidspunkt, avviksType)

fun Periode.copy(
    startet: Metadata? = null,
    avsluttet: Metadata? = null
) = Periode(
    this.id,
    this.identitetsnummer,
    startet ?: this.startet,
    avsluttet ?: this.avsluttet
)

fun Metadata.copy(
    tidspunkt: Instant? = null,
    utfoertAv: Bruker? = null,
    kilde: String? = null,
    aarsak: String? = null,
    tidspunktFraKilde: TidspunktFraKilde? = null,
) = Metadata(
    tidspunkt ?: this.tidspunkt,
    utfoertAv ?: this.utfoertAv,
    kilde ?: this.kilde,
    aarsak ?: this.aarsak,
    tidspunktFraKilde ?: this.tidspunktFraKilde
)
