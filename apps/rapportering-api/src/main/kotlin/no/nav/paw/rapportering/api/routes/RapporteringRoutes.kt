package no.nav.paw.rapportering.api.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest
import no.nav.paw.rapportering.api.kafka.RapporteringTilgjengeligState
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

fun Route.rapporteringRoutes(
    kafkaKeyClient: KafkaKeysClient,
    rapporteringStateStoreName: String,
    rapporteringStateStore: ReadOnlyKeyValueStore<Long, RapporteringTilgjengeligState>,
    kafkaStreams: KafkaStreams,
    httpClient: HttpClient
) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<RapporteringRequest>("/rapportering") { rapportering ->
                val identitetsnummer = call.getIdentitetsnummerFromPid()
                    ?: rapportering.identitetsnummer
                    ?: throw IllegalArgumentException("Identitetsnummer mangler")

                val arbeidsoekerId = kafkaKeyClient.getIdAndKey(identitetsnummer)?.id
                    ?: throw IllegalArgumentException("Fant ikke arbeidsoekerId for identitetsnummer")

                val metadata =
                    kafkaStreams.queryMetadataForKey(
                        rapporteringStateStoreName,
                        arbeidsoekerId,
                        Serdes.Long().serializer()
                    )

                // NOT_AVAILABLE if Kafka Streams is (re-)initializing, or null if no matching metadata could be found.
                if (metadata === KeyQueryMetadata.NOT_AVAILABLE) {
                    call.respond(404)
                    return@post
                }

                if (metadata.activeHost().host() == "localhost") {
                    rapporteringStateStore.get(arbeidsoekerId).rapporteringer
                        .find { it.rapporteringsId == rapportering.rapporteringsId }
                        ?.let {
                            call.respond(200)
                        }
                } else {
                    val response = httpClient.post("http://${metadata.activeHost().host()}:8080/api/v1/rapportering") {
                        call.request.headers["Authorization"]?.let { bearerAuth(it) }
                        setBody(rapportering)
                    }
                    call.respond(response.status)
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

fun ApplicationCall.getIdentitetsnummerFromPid(): String? =
    this.authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("tokenx")
        ?.getStringClaim("pid")
