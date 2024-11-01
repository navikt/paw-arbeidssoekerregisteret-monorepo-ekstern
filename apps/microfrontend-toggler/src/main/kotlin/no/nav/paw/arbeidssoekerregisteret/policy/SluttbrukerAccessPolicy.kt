package no.nav.paw.arbeidssoekerregisteret.policy

import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy

class SluttbrukerAccessPolicy : AccessPolicy {

    override fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision {
        val (bruker, _) = context.securityContext

        return when (bruker) {
            is Sluttbruker -> {
                // TODO Håndtere verge
                Permit("Sluttbruker har tilgang")
            }

            else -> {
                Deny("Ingen tilgang for brukergruppe ${bruker::class.simpleName}")
            }
        }
    }
}