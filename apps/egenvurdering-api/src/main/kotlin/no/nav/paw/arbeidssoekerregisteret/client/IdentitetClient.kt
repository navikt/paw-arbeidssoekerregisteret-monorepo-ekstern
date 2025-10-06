package no.nav.paw.arbeidssoekerregisteret.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.security.texas.TexasClient

data class IdentitetClientConfig(val baseUrl: String)

const val identiteterPath = "/api/v2/identiteter"

class IdentitetClient(
    private val config: IdentitetClientConfig,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient = createHttpClient(),
) {
    suspend fun hentIdentiteter(
        request: IdentitetRequest,
        visKonflikter: Boolean = false,
        hentPdl: Boolean = false,
        traceparent: String? = null,
    ): IdentitetResponse {
        val response = httpClient.post(config.baseUrl + identiteterPath) {
            contentType(Application.Json)
            bearerAuth(texasClient.getMachineToMachineToken().accessToken)
            if (traceparent != null) header("traceparent", traceparent)

            url {
                if (hentPdl) parameters.append("hentPdl", "true")
                if (visKonflikter) parameters.append("visKonflikter", "true")
            }
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            val status = response.status
            val problemDetails = runCatching { response.body<ProblemDetails>() }.getOrNull()
            val suffix = problemDetails?.let { "(${it.title}: ${it.detail})" } ?: ""
            throw IdentitetClientException(
                "Kall mot $identiteterPath feilet. Statuskode: ${status.value} $suffix"
            )
        }

        return response.body<IdentitetResponse>()
    }
}

class IdentitetClientException(message: String) : RuntimeException(message)
