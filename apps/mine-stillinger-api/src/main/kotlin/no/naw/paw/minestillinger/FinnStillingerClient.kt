package no.naw.paw.minestillinger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.obo.OnBehalfOfBrukerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.ledigestillinger.model.SortOrder

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
    suspend fun finnLedigeStillinger(
        token: AccessToken,
        finnStillingerRequests: List<FinnStillingerRequest>
    ): FinnStillingerResponse {
        val newToken = texasClient.exchangeOnBehalfOfBrukerToken(
            OnBehalfOfBrukerRequest(
                userToken = token.jwt,
                target = config.target
            )
        ).accessToken

        return coroutineScope {
            val svar = finnStillingerRequests.asFlow()
                .map { request ->
                    async {
                        val response = httpClient.post(config.baseUrl + FINN_LEDIGE_STILLINGER_PATH) {
                            contentType(Application.Json)
                            bearerAuth(newToken)
                            setBody(request)
                        }
                        when {
                            response.status.isSuccess() -> response.body<FinnStillingerResponse>()
                            else -> throw StillingerClientException(response.status)
                        }
                    }
                }.toList()
                .awaitAll()
            val stillinger = svar.map { it.stillinger }.interleave()
            FinnStillingerResponse(
                stillinger = stillinger,
                paging = PagingResponse(
                    page = 1,
                    pageSize = (svar.firstOrNull()?.paging?.pageSize ?: 0) *
                            finnStillingerRequests.size,
                    hitSize = stillinger.size,
                    sortOrder = svar.firstOrNull()?.paging?.sortOrder ?: SortOrder.DESC,
                )
            )
        }
    }
}

class StillingerClientException(override val status: HttpStatusCode) : ClientResponseException(
    status = status,
    message = "Feil ved henting av ledige stillinger",
    type = ErrorType.domain("stillinger").error("henting_feilet").build(),
    cause = null
)

fun <T> List<List<T>>.interleave(): List<T> {
    val maxSize = maxOfOrNull { it.size } ?: return emptyList()
    return (0 until maxSize).flatMap { index ->
        this.mapNotNull { list -> list.getOrNull(index) }
    }
}

