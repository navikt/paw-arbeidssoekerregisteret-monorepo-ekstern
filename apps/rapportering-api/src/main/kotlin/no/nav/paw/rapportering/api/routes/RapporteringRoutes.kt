package no.nav.paw.rapportering.api.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
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
import no.nav.paw.rapportering.api.utils.logger
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

                val rapporteringsIdFunnet = rapporteringStateStore.get(arbeidsoekerId)
                    ?.rapporteringer
                    ?.any {
                        it.rapporteringsId == rapportering.rapporteringsId
                                && it.arbeidssoekerId == arbeidsoekerId
                    } == true

                if (rapporteringsIdFunnet) {
                    logger.info("Rapportering med id ${rapportering.rapporteringsId} funnet")
                    return@post call.respond(HttpStatusCode.OK)
                }

                val metadata = kafkaStreams.queryMetadataForKey(
                    rapporteringStateStoreName, arbeidsoekerId, Serdes.Long().serializer()
                )

                if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
                    logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
                    return@post call.respond(HttpStatusCode.NotFound)
                } else {
                    val response = httpClient.post("http://${metadata.activeHost().host()}:8080/api/v1/rapportering") {
                        call.request.headers["Authorization"]?.let { bearerAuth(it) }
                        setBody(rapportering)
                    }
                    return@post call.respond(response.status)
                }
            }
        }
    }
}

fun ApplicationCall.getIdentitetsnummerFromPid(): String? =
    this.authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("tokenx")
        ?.getStringClaim("pid")