package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata


fun periode(
    id: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678911",
    startet: ApiMetadata = metadata(),
    avsluttet: ApiMetadata? = null
) = Periode(id, identitetsnummer, startet, avsluttet)

fun opplysninger(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    sendtIn: String = "2020-01-28",
    utdanning: Utdanning = utdanning(),
    helseHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI,
    jobbsituasjon: Jobbsituasjon = jobbsituasjon(),
    annetHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI
) = OpplysningerOmArbeidssoeker(
    id,
    periodeId,
    metadata(LocalDate.parse(sendtIn).atStartOfDay().toInstant(ZoneOffset.UTC) + Duration.ofHours(6)),
    utdanning,
    helse(helseHindrerArbeid),
    jobbsituasjon,
    annet(annetHindrerArbeid)
)
fun opplysninger(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    sendtInAv: Metadata = metadata(),
    utdanning: Utdanning = utdanning(),
    helse: Helse = helse(),
    jobbsituasjon: Jobbsituasjon = jobbsituasjon(),
    annet: Annet = annet()
) = OpplysningerOmArbeidssoeker(
    id,
    periodeId,
    sendtInAv,
    utdanning,
    helse,
    jobbsituasjon,
    annet
)

fun utdanning(
    utdanning: String = "3",
    godkjent: JaNeiVetIkke = JaNeiVetIkke.JA,
    bestaatt: JaNeiVetIkke = JaNeiVetIkke.JA
) = Utdanning(utdanning, godkjent, bestaatt)

fun helse(
    helseHinder: JaNeiVetIkke = JaNeiVetIkke.NEI
) = Helse(helseHinder)

fun jobbsituasjon(
    jobbsituasjon: List<BeskrivelseMedDetaljer> = emptyList()
) = Jobbsituasjon(jobbsituasjon)

fun annet(
    annet: JaNeiVetIkke = JaNeiVetIkke.NEI
) = Annet(annet)

fun metadata(
    tidspunkt: Instant = Instant.now(),
    bruker: Bruker = bruker()
) = ApiMetadata(
    tidspunkt,
    bruker,
    "1",
    "2",
    null
)

fun bruker(
    type: BrukerType = BrukerType.SYSTEM,
    id: String = "test"
) = Bruker(type, id)