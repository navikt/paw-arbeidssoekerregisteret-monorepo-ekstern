package no.nav.paw.arbeidssoekerregisteret.policy

import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy

class SluttbrukerAccessPolicy : AccessPolicy {
    override suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        return when (val bruker = securityContext.bruker) {
            is Sluttbruker -> Permit("Sluttbruker har tilgang")

            else -> Deny("Ingen tilgang for brukergruppe ${bruker::class.simpleName}")
        }
    }
}