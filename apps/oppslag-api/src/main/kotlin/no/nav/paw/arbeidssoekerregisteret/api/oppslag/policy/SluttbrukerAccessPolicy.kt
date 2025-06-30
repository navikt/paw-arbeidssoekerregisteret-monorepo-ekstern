package no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy
import org.slf4j.LoggerFactory
import java.util.*

class SluttbrukerAccessPolicy(
    private val periodeId: UUID?,
    private val harPeriodeTilgangFunction: (UUID, Collection<Identitetsnummer>) -> Boolean,
) : AccessPolicy {
    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")

    override suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        val (bruker, _) = securityContext

        logger.info("Autoriserer brukertype {}", bruker::class.simpleName)

        when (bruker) {
            is Sluttbruker -> {
                if (periodeId != null) {
                    if (!harPeriodeTilgangFunction(periodeId, bruker.alleIdenter)) {
                        return Deny("Periode tilhører ikke sluttbruker")
                    }
                }
                return Permit("Sluttbruker har tilgang")
            }

            else -> {
                return Deny("Bruker er ikke sluttbruker")
            }
        }
    }
}