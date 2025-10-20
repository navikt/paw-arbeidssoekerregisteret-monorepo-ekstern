package no.nav.paw.security.texas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.security.texas.IdentityProvider.AZURE_AD
import no.nav.paw.security.texas.m2m.MachineToMachineTokenRequest
import no.nav.paw.security.texas.m2m.MachineToMachineTokenResponse
import no.nav.paw.security.texas.obo.OnBehalfOfAnsattRequest
import no.nav.paw.security.texas.obo.OnBehalfOfBrukerRequest
import no.nav.paw.security.texas.obo.OnBehalfOfRequest
import no.nav.paw.security.texas.obo.OnBehalfOfResponse

class TexasClient(
    private val config: TexasClientConfig,
    private val httpClient: HttpClient,
) {
    suspend fun exchangeOnBehalfOfBrukerToken(request: OnBehalfOfBrukerRequest) = exchangeToken(request)

    suspend fun exchangeOnBehalfOfAnsattToken(request: OnBehalfOfAnsattRequest) = exchangeToken(request)

    suspend fun getMachineToMachineToken(): MachineToMachineTokenResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(Application.Json)
            setBody(
                MachineToMachineTokenRequest(
                    identityProvider = AZURE_AD.value,
                    target = config.target,
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            throw MachineToMachineTokenException("Klarte ikke å hente M2M-token. Statuskode: ${response.status.value}")
        }
        return response.body<MachineToMachineTokenResponse>()
    }

    private suspend fun exchangeToken(onBehalfOfRequest: OnBehalfOfRequest): OnBehalfOfResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(Application.Json)
            setBody(onBehalfOfRequest)
        }
        if (response.status != HttpStatusCode.OK) {
            throw TokenExchangeException("Klarte ikke å veksle token. Statuskode: ${response.status.value}")
        }
        return response.body<OnBehalfOfResponse>()
    }
}

class TokenExchangeException(message: String) : RuntimeException(message)
class MachineToMachineTokenException(message: String) : RuntimeException(message)
