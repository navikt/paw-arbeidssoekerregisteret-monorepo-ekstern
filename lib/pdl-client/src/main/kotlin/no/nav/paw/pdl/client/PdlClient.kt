package no.nav.paw.pdl.client

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import no.nav.paw.client.factory.createObjectMapper
import org.slf4j.LoggerFactory
import java.net.URI

// Se https://pdldocs-navno.msappproxy.net/ for dokumentasjon av PDL API-et
class PdlClient(
    url: String,
    // Tema: https://confluence.adeo.no/pages/viewpage.action?pageId=309311397
    private val tema: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String,
) {
    internal val logger = LoggerFactory.getLogger(this::class.java)

    private val graphQLClient = GraphQLKtorClient(
        url = URI.create(url).toURL(),
        httpClient = httpClient,
        serializer = GraphQLClientJacksonSerializer(createObjectMapper())
    )

    internal suspend fun <T : Any> execute(
        query: GraphQLClientRequest<T>,
        behandlingsnummer: String,
        callId: String?,
        traceparent: String? = null,
        navConsumerId: String?,
    ): GraphQLClientResponse<T> {
        val safeBehandlingsnummer = require(behandlingsnummer.isNotBlank()) { "Behandlingsnummer kan ikke være tom" }
        return graphQLClient.execute(query) {
            bearerAuth(getAccessToken())
            header("Tema", tema)
            header("Nav-Call-Id", callId)
            header("Nav-Consumer-Id", navConsumerId)
            header("Behandlingsnummer", safeBehandlingsnummer)
            traceparent?.let { header("traceparent", it) }
        }
    }
}

fun <T> GraphQLClientResponse<T>.hasNotFoundError(): Boolean {
    return errors
        ?.mapNotNull { it.extensions }
        ?.mapNotNull { it["code"] }
        ?.map { it.toString() }
        ?.contains("not_found")
        ?: false
}
