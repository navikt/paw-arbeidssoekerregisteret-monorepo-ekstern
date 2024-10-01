package no.nav.paw.arbeidssoekerregisteret.api.oppslag.models

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerType as RestBrukerType

fun ProfilertTil.toProfileringsResultat() =
    when (this) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
        ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
        ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
    }

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
        this.brukerId
    )

fun TidspunktFraKildeRow.toTidspunktFraKilde() =
    TidspunktFraKilde(
        this.tidspunkt,
        this.avviksType
    )

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

fun MetadataRow.toMetadataResponse() =
    MetadataResponse(
        this.tidspunkt,
        this.utfoertAv.toBrukerResponse(),
        this.kilde,
        this.aarsak,
        this.tidspunktFraKilde?.toTidspunktFraKildeResponse()
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
        BrukerType.VEILEDER -> RestBrukerType.VEILEDER
        BrukerType.SLUTTBRUKER -> RestBrukerType.SLUTTBRUKER
        BrukerType.SYSTEM -> RestBrukerType.SYSTEM
        BrukerType.UDEFINERT -> RestBrukerType.UDEFINERT
        BrukerType.UKJENT_VERDI -> RestBrukerType.UKJENT_VERDI
    }

fun AvviksType.toAvviksTypeResponse() =
    when (this) {
        AvviksType.RETTING -> AvviksTypeResponse.RETTING
        AvviksType.FORSINKELSE -> AvviksTypeResponse.FORSINKELSE
        AvviksType.UKJENT_VERDI -> AvviksTypeResponse.UKJENT_VERDI
    }

fun JaNeiVetIkke.toJaNeiVetIkke() =
    no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JaNeiVetIkke.valueOf(this.name)

fun Beskrivelse.toJobbSituasjonBeskrivelse() =
    JobbSituasjonBeskrivelse.valueOf(this.name)
