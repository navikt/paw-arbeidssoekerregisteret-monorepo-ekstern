package no.nav.paw.oppslagapi.dataconsumer.converters

import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv as AvroPaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning as AvroBekreftelsesloesning
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start as AvroStart
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp as AvroStopp

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart as OpenApiPaaVegneAvStart
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning as OpenApiBekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp as OpenApiPaaVegneAvStopp

// Extension function to convert Avro Bekreftelsesloesning to OpenApiBekreftelsesloesning
fun AvroBekreftelsesloesning.toOpenApi(): OpenApiBekreftelsesloesning =
    when(this) {
        AvroBekreftelsesloesning.UKJENT_VERDI -> OpenApiBekreftelsesloesning.UKJENT_VERDI
        AvroBekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> OpenApiBekreftelsesloesning.ARBEIDSSOEKERREGISTERET
        AvroBekreftelsesloesning.DAGPENGER -> OpenApiBekreftelsesloesning.DAGPENGER
        AvroBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> OpenApiBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
    }

// Extension function to convert Avro Start to OpenApiPaaVegneAvHandling
fun AvroStart.toOpenApi(avroPaVegneAv: AvroPaaVegneAv): OpenApiPaaVegneAvStart =
    OpenApiPaaVegneAvStart(
        intervalMS = this.intervalMS,
        graceMS = this.graceMS,
        periodeId = avroPaVegneAv.periodeId,
        bekreftelsesloesning = avroPaVegneAv.bekreftelsesloesning.toOpenApi()
    )

// Extension function to convert Avro Stopp to OpenApiPaaVegneAvHandling
fun AvroStopp.toOpenApi(avroPaaVegneAv: AvroPaaVegneAv): OpenApiPaaVegneAvStopp =
    OpenApiPaaVegneAvStopp(
        periodeId = avroPaaVegneAv.periodeId,
        bekreftelsesloesning = avroPaaVegneAv.bekreftelsesloesning.toOpenApi(),
        fristBrutt = this.fristBrutt
    )
