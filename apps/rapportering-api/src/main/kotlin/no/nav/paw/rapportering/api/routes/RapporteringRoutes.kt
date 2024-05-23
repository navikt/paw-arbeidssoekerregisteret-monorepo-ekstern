package no.nav.paw.rapportering.api.routes

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest

fun Route.rapporteringRoutes(kafkaKeyGeneratorClient: ) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<RapporteringRequest>("/rapportering") { rapportering ->
                // sjekke periodeId opp mot identitetsnummer
                // RapporteringTilgjengelig -> kan rapportere
                // RapporteringsMeldingMottatt eller PeriodeAvsluttet -> sletter rapporteringsmulighet

                // KafkaStreams, lagrer rapportering tilgjengelig i state store
                // Fjerner rapportering tilgjengelig når rapportering er mottatt
                // Fjerner all rapportering tilgjengelig for bruker når periode er avsluttet

                // sende rapportering ut på rapporteringstopic -> producer
                call.respond("Rapportering mottatt")
            }
        }
    }
}