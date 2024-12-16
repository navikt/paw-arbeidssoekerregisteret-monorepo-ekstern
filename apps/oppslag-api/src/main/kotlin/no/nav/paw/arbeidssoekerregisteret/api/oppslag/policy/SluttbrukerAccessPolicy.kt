package no.nav.paw.arbeidssoekerregisteret.api.oppslag.policy

import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy
import java.util.*

class SluttbrukerAccessPolicy(
    private val periodeId: UUID?,
    private val harPeriodeTilgangFunction: (UUID, Collection<Identitetsnummer>) -> Boolean,
) : AccessPolicy {

    override fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        val (bruker, _) = securityContext

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