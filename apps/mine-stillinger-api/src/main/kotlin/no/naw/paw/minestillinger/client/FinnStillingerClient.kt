package no.naw.paw.minestillinger.client

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
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse

data class LedigeStillingerClientConfig(val baseUrl: String)

const val LEDIGE_STILLINGER_PATH = "/api/v1/stillinger"

class FinnStillingerClient(
    private val config: LedigeStillingerClientConfig,
    private val tokenProvider: (String) -> String,
    private val httpClient: HttpClient,
) {
    suspend fun hentLedigeStillinger(finnStillingerRequest: FinnStillingerRequest): FinnStillingerResponse {
        val response = httpClient.post(config.baseUrl + LEDIGE_STILLINGER_PATH) {
            contentType(Application.Json)
            bearerAuth("TODO")
            setBody(finnStillingerRequest)
        }

        return when {
            response.status.isSuccess() -> response.body<FinnStillingerResponse>()
            else -> {
                TODO("Implementer meeeg")
            }
        }
    }
}

/*
class StillingerClientException(override val status: HttpStatusCode) : ClientResponseException(
    status = status,
    message = "Feil ved henting av ledige stillinger",
    type = ErrorType.domain("stillinger").error("henting_feilet").build(),
    cause =
)*/
