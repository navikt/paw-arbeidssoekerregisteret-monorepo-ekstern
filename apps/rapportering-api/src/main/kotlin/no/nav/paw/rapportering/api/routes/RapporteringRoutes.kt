package no.nav.paw.rapportering.api.routes

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest
import no.nav.paw.rapportering.api.utils.TokenXPID
import requestScope

fun Route.rapporteringRoutes(kafkaKeyClient: KafkaKeysClient) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<RapporteringRequest>("/rapportering") { rapportering ->
                with(requestScope()) {
                    println(claims)
                    val pid = claims[TokenXPID]

                    // if veileder -> identitetsnummer må sendes med i request body
                    // if tokenx -> identitetsnummer fra token
                }
                // sjekke rapportId opp mot identitetsnummer
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