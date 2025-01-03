package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PoaoTilgangHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy.SluttbrukerAccessPolicy
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy.VeilederAccessPolicy
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.poao_tilgang.api.dto.response.DecisionType
import java.util.*

class AuthorizationService(
    private val serverConfig: ServerConfig,
    private val periodeRepository: PeriodeRepository,
    private val pdlHttpConsumer: PdlHttpConsumer,
    private val poaoTilgangHttpConsumer: PoaoTilgangHttpConsumer
) {
    private val logger = buildLogger

    fun sluttbrukerAccessPolicies(
    ): List<AccessPolicy> = sluttbrukerAccessPolicies(null)

    fun sluttbrukerAccessPolicies(periodeId: UUID?): List<AccessPolicy> {
        return listOf(
            SluttbrukerAccessPolicy(periodeId, this::harPeriodeTilgang),
        )
    }

    suspend fun veilederAccessPolicies(periodeId: UUID): List<AccessPolicy> {
        val periodeRow = hentPeriodeRow(periodeId)
        val identiteter = finnIdentiteter(Identitetsnummer(periodeRow.identitetsnummer))
        return veilederAccessPolicies(periodeId, identiteter)
    }

    fun veilederAccessPolicies(
        identiteter: Collection<Identitetsnummer>
    ): List<AccessPolicy> {
        return veilederAccessPolicies(null, identiteter)
    }

    fun veilederAccessPolicies(
        periodeId: UUID?, identiteter: Collection<Identitetsnummer>
    ): List<AccessPolicy> {
        return listOf(
            VeilederAccessPolicy(
                serverConfig.runtimeEnvironment,
                identiteter,
                periodeId,
                this::harSluttbrukerTilgang,
                this::harPeriodeTilgang
            )
        )
    }

    suspend fun utvidPrincipal(securityContext: SecurityContext): SecurityContext {
        return when (val bruker = securityContext.bruker) {
            is Sluttbruker -> {
                val alleIdenter = finnIdentiteter(bruker.ident).toHashSet()
                SecurityContext(
                    bruker = Sluttbruker(bruker.ident, alleIdenter), accessToken = securityContext.accessToken
                )
            }

            else -> securityContext
        }
    }

    private fun hentPeriodeRow(periodeId: UUID): PeriodeRow {
        return periodeRepository.hentPeriodeForId(periodeId)
            ?: throw PeriodeIkkeFunnetException("Finner ikke periode for periodeId")
    }

    suspend fun finnIdentiteter(
        identitetsnummer: Identitetsnummer, identGruppe: IdentGruppe = IdentGruppe.FOLKEREGISTERIDENT
    ): List<Identitetsnummer> {
        return pdlHttpConsumer.finnIdenter(identitetsnummer).filter { it.gruppe == identGruppe }
            .map { Identitetsnummer(it.ident) }
    }

    private fun harPeriodeTilgang(
        periodeId: UUID, identiteter: Collection<Identitetsnummer>
    ): Boolean {
        logger.debug("Verifiserer tilgang til periode")

        val periodeRow = hentPeriodeRow(periodeId)
        return periodeRow.identitetsnummer.let { identiteter.contains(Identitetsnummer(it)) }
    }

    private fun harSluttbrukerTilgang(
        bruker: NavAnsatt,
        identiteter: Collection<Identitetsnummer>,
        action: Action
    ): Boolean = runBlocking {
        logger.debug("Verifiserer at veileder har {}-tilgang til sluttbruker", action.name)
        val response = poaoTilgangHttpConsumer.evaluatePolicies(bruker, identiteter, action)
        val (permit, deny) = response.results.partition { it.decision.type == DecisionType.PERMIT }
        if (permit.isNotEmpty() && deny.isNotEmpty()) {
            logger.warn("POAO Tilgang returnerte et hetrogent svar")
        }
        deny.isEmpty()
    }


}
