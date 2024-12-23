package no.nav.paw.poao.tilgang.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.poao_tilgang.api.dto.request.EvaluatePoliciesRequest
import no.nav.poao_tilgang.api.dto.response.EvaluatePoliciesResponse
import org.slf4j.LoggerFactory

class PoaoTilgangClient(
    private val url: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun evaluatePolicies(request: EvaluatePoliciesRequest<Any>): EvaluatePoliciesResponse {
        logger.debug("Henter tilganger fra POAO Tilgang")
        val response = httpClient.post("${url}/api/v1/policy/evaluate") {
            contentType(ContentType.Application.Json)
            bearerAuth(getAccessToken())
            setBody(request)
        }
        logger.debug("Hentet tilganger fra POAO Tilgang")
        return response.call.body()
    }
}