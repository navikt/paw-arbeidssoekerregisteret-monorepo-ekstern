package no.nav.paw.arbeidssoekerregisteret.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paw.arbeidssoekerregisteret.config.TexasClientConfig
import no.nav.paw.client.factory.createHttpClient

class TexasOnBehalfOfClient(
    private val config: TexasClientConfig,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun getOnBehalfOfToken(userToken: String): OnBehalfOfResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                OnBehalfOfRequest(
                    user_token = userToken,
                    identity_provider = config.identityProvider,
                    target = config.target
                )
            )
        }
        return response.body<OnBehalfOfResponse>().also {
            if (response.status.value != 200) {
                throw RuntimeException("Failed to get on behalf of token: ${response.status.value} - $it")
            }
        }
    }
}

data class OnBehalfOfRequest(
    val user_token: String,
    val identity_provider: String,
    val target: String,
)

data class OnBehalfOfResponse(
    val access_token: String,
)