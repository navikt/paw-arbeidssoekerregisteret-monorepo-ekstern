package no.nav.paw.oppslagapi

import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv as AvroPaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning as AvroBekreftelsesloesning
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start as AvroStart
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp as AvroStopp

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAv as OpenApiPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning as OpenApiBekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvHandling as OpenApiPaaVegneAvHandling

// Extension function to convert Avro Bekreftelsesloesning to OpenApiBekreftelsesloesning
fun AvroBekreftelsesloesning.toOpenApi(): OpenApiBekreftelsesloesning =
    when(this) {
        AvroBekreftelsesloesning.UKJENT_VERDI -> OpenApiBekreftelsesloesning.UKJENT_VERDI
        AvroBekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> OpenApiBekreftelsesloesning.ARBEIDSSOEKERREGISTERET
        AvroBekreftelsesloesning.DAGPENGER -> OpenApiBekreftelsesloesning.DAGPENGER
        AvroBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> OpenApiBekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
    }

// Extension function to convert Avro Start to OpenApiPaaVegneAvHandling
fun AvroStart.toOpenApi(): OpenApiPaaVegneAvHandling =
    OpenApiPaaVegneAvHandling(
        intervalMS = this.intervalMS,
        graceMS = this.graceMS,
        fristBrutt = false
    )

// Extension function to convert Avro Stopp to OpenApiPaaVegneAvHandling
fun AvroStopp.toOpenApi(): OpenApiPaaVegneAvHandling =
    OpenApiPaaVegneAvHandling(
        intervalMS = 0,
        graceMS = 0,
        fristBrutt = this.fristBrutt
    )

// Extension function to convert Avro PaaVegneAv to OpenApiPaaVegneAv
fun AvroPaaVegneAv.toOpenApi(): OpenApiPaaVegneAv =
    OpenApiPaaVegneAv(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.toOpenApi(),
        handling = when (val h = this.handling) {
            is AvroStart -> h.toOpenApi()
            is AvroStopp -> h.toOpenApi()
            else -> throw IllegalArgumentException("Unknown handling type: ${h?.javaClass}")
        }
    )
