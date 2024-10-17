package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.audit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildAuditLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.poao_tilgang.client.NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PolicyRequest
import java.util.*

class AuthorizationService(
    private val pdlHttpConsumer: PdlHttpConsumer,
    private val poaoTilgangHttpClient: PoaoTilgangCachedClient
) {
    private val logger = buildLogger
    private val auditLogger = buildAuditLogger

    suspend fun finnIdentiteter(
        identitetsnummer: Identitetsnummer,
        identGruppe: IdentGruppe = IdentGruppe.FOLKEREGISTERIDENT
    ): List<Identitetsnummer> {
        return pdlHttpConsumer.finnIdenter(identitetsnummer)
            .filter { it.gruppe == identGruppe }
            .map { Identitetsnummer(it.ident) }
    }

    fun verifiserTilgangTilBruker(
        navAnsatt: NavAnsatt,
        identitetsnummerList: List<Identitetsnummer>,
    ): Boolean {
        val policyRequests = identitetsnummerList
            .map { NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput(navAnsatt.navIdent, it.verdi) }
            .map { PolicyRequest(UUID.randomUUID(), it) }
        val result = poaoTilgangHttpClient.evaluatePolicies(policyRequests).getOrThrow()
        val (permit, deny) = result.partition { it.decision.isPermit }
        if (permit.isNotEmpty() && deny.isNotEmpty()) {
            logger.warn("POAO Tilgang returnerte et hetrogent svar")
        }

        return deny.isEmpty().also { harTilgang ->
            if (harTilgang) {
                auditLogger.audit(
                    identitetsnummerList.first(),
                    navAnsatt,
                    "NAV ansatt har hentet informasjon om bruker"
                )
            } else {
                logger.info("NAV-ansatt har ikke tilgang til bruker")
            }
        }
    }
}
