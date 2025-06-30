package no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.audit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildAuditLogger
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.model.Identitetsnummer
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
    private val harSluttbrukerTilgangFunction: (NavAnsatt, Collection<Identitetsnummer>, Action) -> Boolean,
    private val harPeriodeTilgangFunction: (UUID, Collection<Identitetsnummer>) -> Boolean
) : AccessPolicy {
    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")
    private val auditLogger: Logger = buildAuditLogger

    override suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        val (bruker, _) = securityContext

        logger.info("Autoriserer brukertype {}", bruker::class.simpleName)

        return when (bruker) {
            is NavAnsatt -> {
                hasNavAnsattAccess(action, bruker)
            }

            is Anonym -> {
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

        if (!harSluttbrukerTilgang(action, bruker)) {
            return Deny("NAV-ansatt har ikke $action-tilgang til sluttbruker")
        }

        if (!harPeriodeTilgang()) {
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

    private fun hasM2MTokenAccess(action: Action, bruker: Anonym): AccessDecision {
        if (identiteter.isEmpty()) {
            return Deny("M2M-token må sende med identitetsnummer for sluttbruker")
        }

        if (!harPeriodeTilgang()) {
            return Deny("Periode tilhører ikke angitt sluttbruker")
        }

        return Permit("M2M-token har $action-tilgang til sluttbruker")
    }

    private fun harSluttbrukerTilgang(action: Action, bruker: NavAnsatt): Boolean =
        harSluttbrukerTilgangFunction(bruker, identiteter, action)

    private fun harPeriodeTilgang(): Boolean {
        return if (periodeId == null) {
            true
        } else {
            harPeriodeTilgangFunction(periodeId, identiteter)
        }
    }
}