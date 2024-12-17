package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PoaoTilgangHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.audit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildAuditLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authorization.model.Action
import no.nav.poao_tilgang.client.NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PolicyRequest
import java.util.*

class AuthorizationService(
    private val serverConfig: ServerConfig,
    private val pdlHttpConsumer: PdlHttpConsumer,
    private val poaoTilgangHttpConsumer: PoaoTilgangHttpConsumer
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
        identitetsnummerList: Collection<Identitetsnummer>,
    ): Boolean {
        val policyRequests = identitetsnummerList
            .map { NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput(navAnsatt.ident, it.verdi) }
            .map { PolicyRequest(UUID.randomUUID(), it) }
        val result = poaoTilgangHttpConsumer.evaluatePolicies(policyRequests)
        val (permit, deny) = result.partition { it.decision.isPermit }
        if (permit.isNotEmpty() && deny.isNotEmpty()) {
            logger.warn("POAO Tilgang returnerte et hetrogent svar")
        }

        return deny.isEmpty().also { harTilgang ->
            if (harTilgang) {
                auditLogger.audit(
                    runtimeEnvironment = serverConfig.runtimeEnvironment,
                    aktorIdent = navAnsatt.ident,
                    identitetsnummerList.first().verdi,
                    action = Action.READ,
                    melding = "NAV ansatt har hentet informasjon om bruker"
                )
            } else {
                logger.info("NAV-ansatt har ikke tilgang til bruker")
            }
        }
    }
}
