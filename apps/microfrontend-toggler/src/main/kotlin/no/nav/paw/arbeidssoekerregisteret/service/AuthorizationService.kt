package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.policy.SluttbrukerAccessPolicy
import no.nav.paw.security.authorization.policy.AccessPolicy

class AuthorizationService {

    fun accessPolicies(): List<AccessPolicy> {
        return listOf(
            SluttbrukerAccessPolicy()
        )
    }
}

