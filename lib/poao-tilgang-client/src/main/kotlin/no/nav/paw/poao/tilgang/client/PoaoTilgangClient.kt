package no.nav.paw.poao.tilgang.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.poao_tilgang.api.dto.response.DecisionDto
import no.nav.poao_tilgang.api.dto.response.DecisionType
import no.nav.poao_tilgang.api.dto.response.PolicyEvaluationResultDto
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import no.nav.poao_tilgang.client.toRequestDto

class PoaoTilgangClient(
    private val url: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String
) {

    suspend fun evaluatePolicies(requests: List<PolicyRequest>): List<PolicyResult> {
        val requestBody = requests.map { toRequestDto(it) }
        val response = httpClient.post("${url}/api/v1/policy/evaluate") {
            contentType(ContentType.Application.Json)
            bearerAuth(getAccessToken())
            setBody(requestBody)
        }
        val responseBody: List<PolicyEvaluationResultDto> = response.call.body()
        return responseBody.map { PolicyResult(it.requestId, it.decision.toDecision()) }
    }

    private fun DecisionDto.toDecision(): Decision {
        return when (this.type) {
            DecisionType.PERMIT -> Decision.Permit
            DecisionType.DENY -> {
                val message = this.message
                val reason = this.reason

                check(message != null) { "message cannot be null" }
                check(reason != null) { "reason cannot be null" }

                Decision.Deny(message, reason)
            }
        }
    }
}