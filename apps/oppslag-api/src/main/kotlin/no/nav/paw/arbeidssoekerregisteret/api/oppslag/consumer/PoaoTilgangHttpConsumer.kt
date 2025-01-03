package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.poao.tilgang.client.PoaoTilgangClient
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authorization.model.Action
import no.nav.poao_tilgang.api.dto.request.EvaluatePoliciesRequest
import no.nav.poao_tilgang.api.dto.request.PolicyEvaluationRequestDto
import no.nav.poao_tilgang.api.dto.request.PolicyId
import no.nav.poao_tilgang.api.dto.request.policy_input.NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInputV1Dto
import no.nav.poao_tilgang.api.dto.request.policy_input.NavAnsattNavIdentSkrivetilgangTilEksternBrukerPolicyInputV1Dto
import no.nav.poao_tilgang.api.dto.response.EvaluatePoliciesResponse
import java.util.*

class PoaoTilgangHttpConsumer(
    private val poaoTilgangClient: PoaoTilgangClient,
) {
    private val logger = buildLogger

    suspend fun evaluatePolicies(
        bruker: NavAnsatt,
        identiteter: Collection<Identitetsnummer>,
        action: Action
    ): EvaluatePoliciesResponse {
        val request = identiteter
            .map { action.asPolicyEvaluationRequest(bruker, it) }
            .let { EvaluatePoliciesRequest(it) }
        return poaoTilgangClient.evaluatePolicies(request)
    }

    private fun Action.asPolicyEvaluationRequest(
        bruker: NavAnsatt,
        identitet: Identitetsnummer
    ): PolicyEvaluationRequestDto<Any> {
        if (this == Action.WRITE) {
            return PolicyEvaluationRequestDto(
                requestId = UUID.randomUUID(),
                policyInput = NavAnsattNavIdentSkrivetilgangTilEksternBrukerPolicyInputV1Dto(
                    navIdent = bruker.ident,
                    norskIdent = identitet.verdi,

                    ),
                policyId = PolicyId.NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1
            )
        } else {
            return PolicyEvaluationRequestDto(
                requestId = UUID.randomUUID(),
                policyInput = NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInputV1Dto(
                    navIdent = bruker.ident,
                    norskIdent = identitet.verdi,

                    ),
                policyId = PolicyId.NAV_ANSATT_NAV_IDENT_LESETILGANG_TIL_EKSTERN_BRUKER_V1
            )
        }
    }
}