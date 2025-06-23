package no.nav.paw.arbeidssoekerregisteret.api.oppslag.models

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
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Bekreftelsesloesning as BekreftelsesloesningResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType as BrukerTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JaNeiVetIkke as JaNeiVetIkkeResponse

fun BekreftelseRow.toBekreftelseResponse(): BekreftelseResponse =
    BekreftelseResponse(
        periodeId = periodeId,
        bekreftelsesloesning = bekreftelsesloesning.toBekreftelsesloesningResponse(),
        svar = svar.toBekreftelseSvarResponse()
    )

private fun BekreftelseSvarRow.toBekreftelseSvarResponse(): BekreftelseSvarResponse =
    BekreftelseSvarResponse(
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        gjelderFra = gjelderFra,
        gjelderTil = gjelderTil,
        harJobbetIDennePerioden = harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker = vilFortsetteSomArbeidssoeker,
    )

fun OpplysningerRow.toOpplysningerOmArbeidssoekerResponse(): OpplysningerOmArbeidssoekerResponse =
    OpplysningerOmArbeidssoekerResponse(
        opplysningerOmArbeidssoekerId = opplysningerId,
        periodeId = periodeId,
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        jobbsituasjon = jobbsituasjon.map { it.toBeskrivelseMedDetaljerResponse() },
        utdanning = utdanning?.toUtdanningResponse(),
        helse = helse?.toHelseResponse(),
        annet = annet?.toAnnetResponse()
    )

private fun BeskrivelseMedDetaljerRow.toBeskrivelseMedDetaljerResponse(): BeskrivelseMedDetaljerResponse =
    BeskrivelseMedDetaljerResponse(
        beskrivelse = beskrivelse.toJobbSituasjonBeskrivelse(),
        detaljer = detaljer.associate { it.noekkel to it.verdi }
    )

private fun UtdanningRow.toUtdanningResponse(): UtdanningResponse =
    UtdanningResponse(
        nus = nus,
        bestaatt = bestaatt?.toJaNeiVetIkkeResponse(),
        godkjent = godkjent?.toJaNeiVetIkkeResponse(),
    )

private fun HelseRow.toHelseResponse(): HelseResponse =
    HelseResponse(
        helsetilstandHindrerArbeid = helsetilstandHindrerArbeid.toJaNeiVetIkkeResponse()
    )

private fun AnnetRow.toAnnetResponse(): AnnetResponse =
    AnnetResponse(
        andreForholdHindrerArbeid = andreForholdHindrerArbeid?.toJaNeiVetIkkeResponse()
    )

fun ProfilertTil.toProfileringsResultat() =
    when (this) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
        ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
        ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
    }

fun OpplysningerRow.toOpplysningerOmArbeidssoeker(): OpplysningerOmArbeidssoeker =
    OpplysningerOmArbeidssoeker(
        opplysningerId,
        periodeId,
        sendtInnAv.toMetadata(),
        utdanning?.toUtdanning(),
        helse?.toHelse(),
        toJobbsituasjon(),
        annet?.toAnnet()
    )

fun OpplysningerRow.toJobbsituasjon(): Jobbsituasjon {
    return Jobbsituasjon(jobbsituasjon.map { it.toBeskrivelseMedDetaljer() })
}

fun BeskrivelseMedDetaljerRow.toBeskrivelseMedDetaljer(): BeskrivelseMedDetaljer =
    BeskrivelseMedDetaljer(beskrivelse, detaljer.associate { it.noekkel to it.verdi })

fun UtdanningRow.toUtdanning(): Utdanning =
    Utdanning(nus, bestaatt, godkjent)

fun HelseRow.toHelse(): Helse =
    Helse(helsetilstandHindrerArbeid)

fun AnnetRow.toAnnet(): Annet =
    Annet(andreForholdHindrerArbeid)

fun PeriodeRow.toPeriode() =
    Periode(
        this.periodeId,
        this.identitetsnummer,
        this.startet.toMetadata(),
        this.avsluttet?.toMetadata()
    )

fun PeriodeRow.toArbeidssoekerperiodeResponse() =
    ArbeidssoekerperiodeResponse(
        this.periodeId,
        this.startet.toMetadataResponse(),
        this.avsluttet?.toMetadataResponse()
    )

fun ProfileringRow.toProfileringResponse(): ProfileringResponse =
    ProfileringResponse(
        profileringId = profileringId,
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        profilertTil = profilertTil.toProfileringsResultat(),
        jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder = alder
    )

fun ProfileringRow.toProfileringAggregertResponse(egenvurdering: EgenvurderingResponse?): ProfileringAggregertResponse =
    ProfileringAggregertResponse(
        profileringId = profileringId,
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        profilertTil = profilertTil.toProfileringsResultat(),
        jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder = alder,
        egenvurdering = egenvurdering
    )

fun EgenvurderingRow.toEgenvurderingResponse(): EgenvurderingResponse =
    EgenvurderingResponse(
        egenvurderingId = egenvurderingId,
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
        profileringId = profileringId,
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        egenvurdering = egenvurdering.toProfileringsResultat(),
    )

fun ProfileringRow.toProfilering(): Profilering =
    Profilering(
        profileringId,
        periodeId,
        opplysningerOmArbeidssoekerId,
        sendtInnAv.toMetadata(),
        profilertTil,
        jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder
    )

fun MetadataRow.toMetadataResponse() =
    MetadataResponse(
        this.tidspunkt,
        this.utfoertAv.toBrukerResponse(),
        this.kilde,
        this.aarsak,
        this.tidspunktFraKilde?.toTidspunktFraKildeResponse()
    )

fun MetadataRow.toMetadata() =
    Metadata(
        this.tidspunkt,
        this.utfoertAv.toBruker(),
        this.kilde,
        this.aarsak,
        this.tidspunktFraKilde?.toTidspunktFraKilde()
    )

fun BrukerRow.toBruker() =
    Bruker(
        this.type,
        this.brukerId,
        null
    )

fun TidspunktFraKildeRow.toTidspunktFraKilde() =
    TidspunktFraKilde(
        this.tidspunkt,
        this.avviksType
    )

fun BrukerRow.toBrukerResponse() =
    BrukerResponse(
        this.type.toBrukerTypeResponse(),
        this.brukerId
    )

fun TidspunktFraKildeRow.toTidspunktFraKildeResponse() =
    TidspunktFraKildeResponse(
        this.tidspunkt,
        this.avviksType.toAvviksTypeResponse()
    )

fun BrukerType.toBrukerTypeResponse() =
    when (this) {
        BrukerType.SLUTTBRUKER -> BrukerTypeResponse.SLUTTBRUKER
        BrukerType.VEILEDER -> BrukerTypeResponse.VEILEDER
        BrukerType.SYSTEM -> BrukerTypeResponse.SYSTEM
        BrukerType.UDEFINERT -> BrukerTypeResponse.UDEFINERT
        BrukerType.UKJENT_VERDI -> BrukerTypeResponse.UKJENT_VERDI
    }

fun AvviksType.toAvviksTypeResponse() =
    when (this) {
        AvviksType.RETTING -> AvviksTypeResponse.RETTING
        AvviksType.FORSINKELSE -> AvviksTypeResponse.FORSINKELSE
        AvviksType.UKJENT_VERDI -> AvviksTypeResponse.UKJENT_VERDI
        AvviksType.SLETTET -> AvviksTypeResponse.SLETTET
        AvviksType.TIDSPUNKT_KORRIGERT -> AvviksTypeResponse.TIDSPUNKT_KORRIGERT
    }

fun JaNeiVetIkke.toJaNeiVetIkkeResponse() =
    when (this) {
        JaNeiVetIkke.JA -> JaNeiVetIkkeResponse.JA
        JaNeiVetIkke.NEI -> JaNeiVetIkkeResponse.NEI
        JaNeiVetIkke.VET_IKKE -> JaNeiVetIkkeResponse.VET_IKKE
    }

fun Beskrivelse.toJobbSituasjonBeskrivelse() =
    when (this) {
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> JobbSituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.ALDRI_HATT_JOBB -> JobbSituasjonBeskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.NY_JOBB -> JobbSituasjonBeskrivelse.NY_JOBB
        Beskrivelse.HAR_SAGT_OPP -> JobbSituasjonBeskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.MIDLERTIDIG_JOBB -> JobbSituasjonBeskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.VIL_BYTTE_JOBB -> JobbSituasjonBeskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> JobbSituasjonBeskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.ER_PERMITTERT -> JobbSituasjonBeskrivelse.ER_PERMITTERT
        Beskrivelse.USIKKER_JOBBSITUASJON -> JobbSituasjonBeskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> JobbSituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.KONKURS -> JobbSituasjonBeskrivelse.KONKURS
        Beskrivelse.ANNET -> JobbSituasjonBeskrivelse.ANNET
        Beskrivelse.UDEFINERT -> JobbSituasjonBeskrivelse.UDEFINERT
        Beskrivelse.UKJENT_VERDI -> JobbSituasjonBeskrivelse.UKJENT_VERDI
    }

fun Bekreftelsesloesning.toBekreftelsesloesningResponse() =
    when (this) {
        Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> BekreftelsesloesningResponse.ARBEIDSSOEKERREGISTERET
        Bekreftelsesloesning.DAGPENGER -> BekreftelsesloesningResponse.DAGPENGER
        Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> BekreftelsesloesningResponse.FRISKMELDT_TIL_ARBEIDSFORMIDLING
        Bekreftelsesloesning.UKJENT_VERDI -> BekreftelsesloesningResponse.UKJENT_VERDI
    }

fun OpplysningerRow.toOpplysningerOmArbeidssoekerAggregertResponse(profilering: ProfileringAggregertResponse?): OpplysningerOmArbeidssoekerAggregertResponse =
    OpplysningerOmArbeidssoekerAggregertResponse(
        opplysningerOmArbeidssoekerId = opplysningerId,
        periodeId = periodeId,
        sendtInnAv = sendtInnAv.toMetadataResponse(),
        jobbsituasjon = jobbsituasjon.map { it.toBeskrivelseMedDetaljerResponse() },
        utdanning = utdanning?.toUtdanningResponse(),
        helse = helse?.toHelseResponse(),
        annet = annet?.toAnnetResponse(),
        profilering = profilering
    )

fun PeriodeRow.toArbeidssoekerPeriodeAggregertResponse(opplysninger: List<OpplysningerOmArbeidssoekerAggregertResponse>, bekreftelser: List<BekreftelseResponse>): ArbeidssoekerperiodeAggregertResponse =
    ArbeidssoekerperiodeAggregertResponse(
        periodeId = periodeId,
        startet = startet.toMetadataResponse(),
        avsluttet = avsluttet?.toMetadataResponse(),
        opplysningerOmArbeidssoeker = opplysninger,
        bekreftelser = bekreftelser
    )
