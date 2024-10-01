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

fun nyOpplysningerOmArbeidssoeker(
    periodeId: UUID = UUID.randomUUID(),
    opplysningerId: UUID = UUID.randomUUID(),
    sendtInAv: Metadata = nyMetadata(bruker = nyBruker(id = "01017012345")),
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
    identitetsnummer: String = "01017012345",
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
    identitetsnummer: String = "01017012345",
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
    periodeId: UUID,
    opplysningerOmArbeidssoekerId: UUID,
    sendtInAv: Metadata = nyMetadata()
) = Profilering(
    UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb"),
    periodeId,
    opplysningerOmArbeidssoekerId,
    sendtInAv,
    ProfilertTil.UDEFINERT,
    true,
    30
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
    id: String = "01017012345"
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
