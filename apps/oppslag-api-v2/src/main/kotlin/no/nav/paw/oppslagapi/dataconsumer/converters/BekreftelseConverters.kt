package no.nav.paw.oppslagapi.dataconsumer.converters

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse as OpenApiBekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning as OpenApiBekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker as OpenApiBruker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata as OpenApiMetadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Svar as OpenApiSvar
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning as AvroBekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker as AvroBruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType as AvroBrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata as AvroMetadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar as AvroSvar

fun Bekreftelse.toOpenApi(): OpenApiBekreftelse =
    OpenApiBekreftelse(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning?.let { toOpenApiBekreftelsesloesning(it) }
            ?: OpenApiBekreftelsesloesning.UKJENT_VERDI,
        id = this.id,
        svar = this.svar.toOpenApi()
    )

private fun toOpenApiBekreftelsesloesning(avro: AvroBekreftelsesloesning): OpenApiBekreftelsesloesning =
    when (avro) {
        AvroBekreftelsesloesning.UKJENT_VERDI -> OpenApiBekreftelsesloesning.UKJENT_VERDI
        AvroBekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> OpenApiBekreftelsesloesning.ARBEIDSSOEKERREGISTERET
        AvroBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> OpenApiBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
        AvroBekreftelsesloesning.DAGPENGER -> OpenApiBekreftelsesloesning.DAGPENGER
    }

private fun AvroSvar.toOpenApi(): OpenApiSvar =
    OpenApiSvar(
        sendtInnAv = this.sendtInnAv.toOpenApi(),
        gjelderFra = this.gjelderFra,
        gjelderTil = this.gjelderTil,
        harJobbetIDennePerioden = this.harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker = this.vilFortsetteSomArbeidssoeker
    )

private fun AvroMetadata.toOpenApi(): OpenApiMetadata =
    OpenApiMetadata(
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.toOpenApi(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = null // Avro.Metadata har ikke tidspunktFraKilde
    )

private fun AvroBruker.toOpenApi(): OpenApiBruker =
    OpenApiBruker(
        type = when (this.type) {
            AvroBrukerType.VEILEDER -> OpenApiBruker.Type.VEILEDER
            AvroBrukerType.SYSTEM -> OpenApiBruker.Type.SYSTEM
            AvroBrukerType.SLUTTBRUKER -> OpenApiBruker.Type.SLUTTBRUKER
            AvroBrukerType.UKJENT_VERDI -> OpenApiBruker.Type.UKJENT_VERDI
            AvroBrukerType.UDEFINERT -> OpenApiBruker.Type.UDEFINERT
        },
        id = this.id,
        sikkerhetsnivaa = this.sikkerhetsnivaa
    )