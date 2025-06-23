package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.*
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Bekreftelsesloesning as BekreftelsesloesningResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType as BrukerTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JaNeiVetIkke as JaNeiVetIkkeResponse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker as BekreftelseBruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType as BekreftelseBrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata as BekreftelseMetadata

infix fun Periode.shouldBeEqualTo(other: ArbeidssoekerperiodeResponse?): Periode {
    other shouldNotBe null
    id shouldBe other?.periodeId
    startet shouldBeEqualTo other?.startet
    avsluttet?.shouldBeEqualTo(other?.avsluttet)
    return this
}

infix fun Periode.shouldBeEqualTo(periode: ArbeidssoekerperiodeAggregertResponse?): Periode {
    periode shouldNotBe null
    id shouldBe periode?.periodeId
    startet shouldBeEqualTo periode?.startet
    avsluttet?.shouldBeEqualTo(periode?.avsluttet)
    return this
}

infix fun OpplysningerOmArbeidssoeker.shouldBeEqualTo(opplysning: OpplysningerOmArbeidssoekerAggregertResponse?): OpplysningerOmArbeidssoeker {
    opplysning shouldNotBe null
    id shouldBe opplysning?.opplysningerOmArbeidssoekerId
    periodeId shouldBe opplysning?.periodeId
    sendtInnAv shouldBeEqualTo opplysning?.sendtInnAv
    for (i in jobbsituasjon.beskrivelser.indices) {
        jobbsituasjon.beskrivelser[i] shouldBeEqualTo opplysning?.jobbsituasjon?.get(i)
    }
    utdanning shouldBeEqualTo opplysning?.utdanning
    helse shouldBeEqualTo opplysning?.helse
    annet shouldBeEqualTo opplysning?.annet
    return this
}

infix fun OpplysningerOmArbeidssoeker.shouldBeEqualTo(other: OpplysningerOmArbeidssoekerResponse?): OpplysningerOmArbeidssoeker {
    other shouldNotBe null
    id shouldBe other?.opplysningerOmArbeidssoekerId
    periodeId shouldBe other?.periodeId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    for (i in jobbsituasjon.beskrivelser.indices) {
        jobbsituasjon.beskrivelser[i] shouldBeEqualTo other?.jobbsituasjon?.get(i)
    }
    utdanning shouldBeEqualTo other?.utdanning
    helse shouldBeEqualTo other?.helse
    annet shouldBeEqualTo other?.annet
    return this
}

infix fun BeskrivelseMedDetaljer.shouldBeEqualTo(other: BeskrivelseMedDetaljerResponse?): BeskrivelseMedDetaljer {
    other shouldNotBe null
    beskrivelse shouldBeEqualTo other?.beskrivelse
    detaljer shouldBe other?.detaljer
    return this
}

infix fun Beskrivelse.shouldBeEqualTo(other: JobbSituasjonBeskrivelse?): Beskrivelse {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun Utdanning.shouldBeEqualTo(other: UtdanningResponse?): Utdanning {
    other shouldNotBe null
    nus shouldBe other?.nus
    bestaatt shouldBeEqualTo other?.bestaatt
    godkjent shouldBeEqualTo other?.godkjent
    return this
}

infix fun Helse.shouldBeEqualTo(other: HelseResponse?): Helse {
    other shouldNotBe null
    helsetilstandHindrerArbeid shouldBeEqualTo other?.helsetilstandHindrerArbeid
    return this
}

infix fun Annet.shouldBeEqualTo(other: AnnetResponse?): Annet {
    other shouldNotBe null
    andreForholdHindrerArbeid shouldBeEqualTo other?.andreForholdHindrerArbeid
    return this
}

infix fun JaNeiVetIkke.shouldBeEqualTo(other: JaNeiVetIkkeResponse?): JaNeiVetIkke {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun Profilering.shouldBeEqualTo(other: ProfileringResponse?): Profilering {
    other shouldNotBe null
    id shouldBe other?.profileringId
    periodeId shouldBe other?.periodeId
    opplysningerOmArbeidssokerId shouldBe other?.opplysningerOmArbeidssoekerId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    profilertTil shouldBeEqualTo other?.profilertTil
    jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe other?.jobbetSammenhengendeSeksAvTolvSisteManeder
    alder shouldBe other?.alder
    return this
}

infix fun Egenvurdering.shouldBeEqualTo(other: EgenvurderingResponse?): Egenvurdering {
    other shouldNotBe null
    id shouldBe other?.egenvurderingId
    periodeId shouldBe other?.periodeId
    opplysningerOmArbeidssokerId shouldBe other?.opplysningerOmArbeidssoekerId
    profileringId shouldBe other?.profileringId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    egenvurdering shouldBeEqualTo other?.egenvurdering
    return this
}

infix fun ProfilertTil.shouldBeEqualTo(other: ProfileringsResultat?): ProfilertTil {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun Metadata.shouldBeEqualTo(other: MetadataResponse?): Metadata {
    other shouldNotBe null
    tidspunkt shouldBe other?.tidspunkt
    utfoertAv shouldBeEqualTo other?.utfoertAv
    kilde shouldBe other?.kilde
    aarsak shouldBe other?.aarsak
    tidspunktFraKilde shouldBeEqualTo other?.tidspunktFraKilde
    return this
}

infix fun Bruker.shouldBeEqualTo(other: BrukerResponse?): Bruker {
    other shouldNotBe null
    type shouldBeEqualTo other?.type
    id shouldBe other?.id
    return this
}

infix fun BrukerType.shouldBeEqualTo(other: BrukerTypeResponse?): BrukerType {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun TidspunktFraKilde.shouldBeEqualTo(other: TidspunktFraKildeResponse?): TidspunktFraKilde {
    other shouldNotBe null
    tidspunkt shouldBe other?.tidspunkt
    avviksType shouldBeEqualTo other?.avviksType
    return this
}

infix fun AvviksType.shouldBeEqualTo(other: AvviksTypeResponse?): AvviksType {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun Bekreftelse.shouldBeEqualTo(other: BekreftelseResponse?): Bekreftelse {
    other shouldNotBe null
    periodeId shouldBe other?.periodeId
    bekreftelsesloesning shouldBeEqualTo other?.bekreftelsesloesning
    svar shouldBeEqualTo other?.svar
    return this
}

infix fun Svar.shouldBeEqualTo(other: BekreftelseSvarResponse?): Svar {
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    gjelderFra shouldBe other?.gjelderFra
    gjelderTil shouldBe other?.gjelderTil
    harJobbetIDennePerioden shouldBe other?.harJobbetIDennePerioden
    vilFortsetteSomArbeidssoeker shouldBe other?.vilFortsetteSomArbeidssoeker
    return this
}

infix fun BekreftelseMetadata.shouldBeEqualTo(other: MetadataResponse?): BekreftelseMetadata {
    other shouldNotBe null
    tidspunkt shouldBe other?.tidspunkt
    utfoertAv shouldBeEqualTo other?.utfoertAv
    kilde shouldBe other?.kilde
    aarsak shouldBe other?.aarsak
    return this
}

infix fun BekreftelseBruker.shouldBeEqualTo(other: BrukerResponse?): BekreftelseBruker {
    type shouldBeEqualTo other?.type
    id shouldBe other?.id
    return this
}

infix fun Bekreftelsesloesning.shouldBeEqualTo(other: BekreftelsesloesningResponse?): Bekreftelsesloesning {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun BekreftelseBrukerType.shouldBeEqualTo(other: BrukerTypeResponse?): BekreftelseBrukerType {
    other shouldNotBe null
    name shouldBe other?.name
    return this
}

infix fun OpplysningerRow.shouldBeEqualTo(other: OpplysningerRow?): OpplysningerRow {
    other shouldNotBe null
    opplysningerId shouldBe other?.opplysningerId
    periodeId shouldBe other?.periodeId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    jobbsituasjon.size shouldBe other?.jobbsituasjon?.size
    for (i in jobbsituasjon.indices) {
        jobbsituasjon[i] shouldBeEqualTo other?.jobbsituasjon?.get(i)
    }
    utdanning?.shouldBeEqualTo(other?.utdanning)
    helse?.shouldBeEqualTo(other?.helse)
    annet?.shouldBeEqualTo(other?.annet)
    return this
}

infix fun BeskrivelseMedDetaljerRow.shouldBeEqualTo(other: BeskrivelseMedDetaljerRow?): BeskrivelseMedDetaljerRow {
    other shouldNotBe null
    beskrivelse shouldBe other?.beskrivelse
    detaljer.size shouldBe other?.detaljer?.size
    for (i in detaljer.indices) {
        detaljer[i] shouldBeEqualTo other?.detaljer?.get(i)
    }
    return this
}

infix fun DetaljerRow.shouldBeEqualTo(other: DetaljerRow?): DetaljerRow {
    other shouldNotBe null
    noekkel shouldBe other?.noekkel
    verdi shouldBe other?.verdi
    return this
}

infix fun UtdanningRow.shouldBeEqualTo(other: UtdanningRow?): UtdanningRow {
    other shouldNotBe null
    nus shouldBe other?.nus
    bestaatt shouldBe other?.bestaatt
    godkjent shouldBe other?.godkjent
    return this
}

infix fun HelseRow.shouldBeEqualTo(other: HelseRow?): HelseRow {
    other shouldNotBe null
    helsetilstandHindrerArbeid shouldBe other?.helsetilstandHindrerArbeid
    return this
}

infix fun AnnetRow.shouldBeEqualTo(other: AnnetRow?): AnnetRow {
    other shouldNotBe null
    andreForholdHindrerArbeid shouldBe other?.andreForholdHindrerArbeid
    return this
}

infix fun PeriodeRow.shouldBeEqualTo(other: PeriodeRow?): PeriodeRow {
    other shouldNotBe null
    periodeId shouldBe other?.periodeId
    startet shouldBeEqualTo other?.startet
    avsluttet?.shouldBeEqualTo(other?.avsluttet)
    return this
}

infix fun ProfileringRow.shouldBeEqualTo(other: ProfileringRow?): ProfileringRow {
    other shouldNotBe null
    profileringId shouldBe other?.profileringId
    periodeId shouldBe other?.periodeId
    opplysningerOmArbeidssoekerId shouldBe other?.opplysningerOmArbeidssoekerId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    profilertTil shouldBe other?.profilertTil
    jobbetSammenhengendeSeksAvTolvSisteManeder shouldBe other?.jobbetSammenhengendeSeksAvTolvSisteManeder
    alder shouldBe other?.alder
    return this
}

infix fun EgenvurderingRow.shouldBeEqualTo(other: EgenvurderingRow?): EgenvurderingRow {
    other shouldNotBe null
    egenvurderingId shouldBe other?.egenvurderingId
    periodeId shouldBe other?.periodeId
    opplysningerOmArbeidssoekerId shouldBe other?.opplysningerOmArbeidssoekerId
    profileringId shouldBe other?.profileringId
    sendtInnAv shouldBeEqualTo other?.sendtInnAv
    egenvurdering shouldBe other?.egenvurdering
    return this
}

infix fun MetadataRow.shouldBeEqualTo(other: MetadataRow?): MetadataRow {
    other shouldNotBe null
    tidspunkt shouldBeEqualTo other?.tidspunkt
    utfoertAv shouldBeEqualTo other?.utfoertAv
    kilde shouldBe other?.kilde
    aarsak shouldBe other?.aarsak
    tidspunktFraKilde?.shouldBeEqualTo(other?.tidspunktFraKilde)
    return this
}

infix fun BrukerRow.shouldBeEqualTo(other: BrukerRow?): BrukerRow {
    other shouldNotBe null
    type shouldBe other?.type
    brukerId shouldBe other?.brukerId
    return this
}

infix fun TidspunktFraKildeRow.shouldBeEqualTo(other: TidspunktFraKildeRow?): TidspunktFraKildeRow {
    other shouldNotBe null
    tidspunkt shouldBeEqualTo other?.tidspunkt
    avviksType.name shouldBe other?.avviksType?.name
    return this
}

infix fun Instant.shouldBeEqualTo(other: Instant?): Instant {
    other shouldNotBe null
    this.truncatedTo(ChronoUnit.SECONDS) shouldBe other?.truncatedTo(ChronoUnit.SECONDS)
    return this
}
