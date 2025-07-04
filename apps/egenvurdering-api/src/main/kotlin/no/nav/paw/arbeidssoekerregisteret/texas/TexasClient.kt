package no.nav.paw.arbeidssoekerregisteret.texas

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.client.factory.createHttpClient

val logger = buildApplicationLogger

class TexasClient(
    private val config: TexasClientConfig,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun getOnBehalfOfToken(userToken: String): OnBehalfOfResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                OnBehalfOfRequest(
                    userToken = userToken,
                    identityProvider = config.identityProvider,
                    target = config.target
                )
            )
        }
        return response.body<OnBehalfOfResponse>().also {
            when {
                response.status == HttpStatusCode.OK -> logger.debug("Token veksling vellykket for bruker.")
                response.status.value != 200 -> {
                    throw TokenExchangeException("Klarte ikke å veksle token. Statuskode: ${response.status.value}")
                }
            }
        }
    }
}

class TokenExchangeException(message: String) : RuntimeException(message)

data class OnBehalfOfRequest(
    @JsonProperty("user_token")
    val userToken: String,
    @JsonProperty("identity_provider")
    val identityProvider: String,
    val target: String,
)

data class OnBehalfOfResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)