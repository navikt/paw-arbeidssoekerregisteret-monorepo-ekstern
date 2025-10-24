package no.naw.paw.minestillinger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.obo.OnBehalfOfBrukerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse

data class LedigeStillingerClientConfig(
    val baseUrl: String,
    val target: String
)

const val FINN_LEDIGE_STILLINGER_PATH = "/api/v1/stillinger"

class FinnStillingerClient(
    private val config: LedigeStillingerClientConfig,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient,
) {
    suspend fun finnLedigeStillinger(token: AccessToken, finnStillingerRequest: FinnStillingerRequest): FinnStillingerResponse {
        val newToken = texasClient.exchangeOnBehalfOfBrukerToken(
            OnBehalfOfBrukerRequest(
                userToken = token.jwt,
                target = config.target
            )
        ).accessToken
        val response = httpClient.post(config.baseUrl + FINN_LEDIGE_STILLINGER_PATH) {
            contentType(Application.Json)
            bearerAuth(newToken)
            setBody(finnStillingerRequest)
        }

        return when {
            response.status.isSuccess() -> response.body<FinnStillingerResponse>()
            else -> throw StillingerClientException(response.status)
        }
    }
}

class StillingerClientException(override val status: HttpStatusCode) : ClientResponseException(
    status = status,
    message = "Feil ved henting av ledige stillinger",
    type = ErrorType.domain("stillinger").error("henting_feilet").build(),
    cause = null
)
