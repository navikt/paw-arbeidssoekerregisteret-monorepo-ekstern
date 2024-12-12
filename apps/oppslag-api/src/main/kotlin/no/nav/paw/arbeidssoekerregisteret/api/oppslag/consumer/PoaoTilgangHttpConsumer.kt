package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult

class PoaoTilgangHttpConsumer(
    private val poaoTilgangClient: PoaoTilgangClient,
) {
    private val logger = buildLogger

    fun evaluatePolicies(policyRequests: List<PolicyRequest>): List<PolicyResult> {
        try {
            logger.debug("Henter tilganger fra POAO Tilgang")
            return poaoTilgangClient.evaluatePolicies(policyRequests).getOrThrow()
        } finally {
            logger.debug("Hentet tilganger fra POAO Tilgang")
        }
    }
}