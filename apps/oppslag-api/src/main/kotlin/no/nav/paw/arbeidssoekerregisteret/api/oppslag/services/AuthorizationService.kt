package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

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
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.TilgangType
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
            SluttbrukerAccessPolicy(periodeId, this::harTilgangTilPeriode),
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
        periodeId: UUID?,
        identiteter: Collection<Identitetsnummer>
    ): List<AccessPolicy> {
        return listOf(
            VeilederAccessPolicy(
                serverConfig.runtimeEnvironment,
                identiteter,
                periodeId,
                this::harVeilederTilgangTilSluttbruker,
                this::harTilgangTilPeriode
            )
        )
    }

    suspend fun utvidPrincipal(securityContext: SecurityContext): SecurityContext {
        return when (val bruker = securityContext.bruker) {
            is Sluttbruker -> {
                val alleIdenter = finnIdentiteter(bruker.ident).toHashSet()
                SecurityContext(
                    bruker = Sluttbruker(bruker.ident, alleIdenter),
                    accessToken = securityContext.accessToken
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
        identitetsnummer: Identitetsnummer,
        identGruppe: IdentGruppe = IdentGruppe.FOLKEREGISTERIDENT
    ): List<Identitetsnummer> {
        return pdlHttpConsumer.finnIdenter(identitetsnummer).filter { it.gruppe == identGruppe }
            .map { Identitetsnummer(it.ident) }
    }

    private fun harTilgangTilPeriode(
        periodeId: UUID,
        identiteter: Collection<Identitetsnummer>
    ): Boolean {
        val periodeRow = hentPeriodeRow(periodeId)
        return periodeRow.identitetsnummer
            .let { identiteter.contains(Identitetsnummer(it)) }
    }

    private fun harVeilederTilgangTilSluttbruker(
        navAnsatt: NavAnsatt,
        identiteter: Collection<Identitetsnummer>,
        action: Action
    ): Boolean {
        val policyRequests = identiteter
            .map { NavAnsattTilgangTilEksternBrukerPolicyInput(navAnsatt.oid, action.asTilgangType(), it.verdi) }
            .map { PolicyRequest(UUID.randomUUID(), it) }
        val result = poaoTilgangHttpConsumer.evaluatePolicies(policyRequests)
        val (permit, deny) = result.partition { it.decision.isPermit }
        if (permit.isNotEmpty() && deny.isNotEmpty()) {
            logger.warn("POAO Tilgang returnerte et hetrogent svar")
        }
        return deny.isEmpty()
    }
}

private fun Action.asTilgangType(): TilgangType = when (this) {
    Action.READ -> TilgangType.LESE
    Action.WRITE -> TilgangType.SKRIVE
}
