package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.security.mock.oauth2.MockOAuth2Server

fun MockOAuth2Server.issueMaskinportenToken(): String {
    return issueToken(
        claims = mapOf(
            "scope" to "nav:arbeid:arbeidssokerregisteret.read"
        )
    ).serialize()
}
