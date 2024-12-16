package no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.audit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildAuditLogger
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.M2MToken
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class VeilederAccessPolicy(
    private val runtimeEnvironment: RuntimeEnvironment,
    private val identiteter: Collection<Identitetsnummer>,
    private val periodeId: UUID?,
    private val harPoaoTilgangFunction: (NavAnsatt, Collection<Identitetsnummer>, Action) -> Boolean,
    private val harPeriodeTilgangFunction: (UUID, Collection<Identitetsnummer>) -> Boolean
) : AccessPolicy {

    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")
    private val auditLogger: Logger = buildAuditLogger

    override fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        val (bruker, _) = securityContext

        return when (bruker) {
            is NavAnsatt -> {
                hasNavAnsattAccess(action, bruker)
            }

            is M2MToken -> {
                hasM2MTokenAccess(action, bruker)
            }

            else -> {
                Deny("Bruker er ikke veileder")
            }
        }
    }

    private fun hasNavAnsattAccess(action: Action, bruker: NavAnsatt): AccessDecision {
        if (identiteter.isEmpty()) {
            return Deny("Veileder må sende med identitetsnummer for sluttbruker")
        }

        if (!harPoaoTilgangFunction(bruker, identiteter, action)) {
            return Deny("NAV-ansatt har ikke $action-tilgang til sluttbruker")
        }

        if (!harTilgangTilPeriode()) {
            return Deny("Periode tilhører ikke angitt sluttbruker")
        }

        auditLogger.audit(
            runtimeEnvironment = runtimeEnvironment,
            aktorIdent = bruker.ident,
            sluttbrukerIdent = identiteter.first().verdi,
            action = action,
            melding = "NAV-ansatt har benyttet $action-tilgang til informasjon om sluttbruker"
        )
        return Permit("Veileder har $action-tilgang til sluttbruker")
    }

    private fun hasM2MTokenAccess(action: Action, bruker: M2MToken): AccessDecision {
        if (identiteter.isEmpty()) {
            return Deny("M2M-token må sende med identitetsnummer for sluttbruker")
        }

        if (!harTilgangTilPeriode()) {
            return Deny("Periode tilhører ikke angitt sluttbruker")
        }

        return Permit("M2M-token har $action-tilgang til sluttbruker")
    }

    private fun harTilgangTilPeriode(): Boolean {
        return if (periodeId == null) {
            true
        } else {
            harPeriodeTilgangFunction(periodeId, identiteter)
        }
    }
}