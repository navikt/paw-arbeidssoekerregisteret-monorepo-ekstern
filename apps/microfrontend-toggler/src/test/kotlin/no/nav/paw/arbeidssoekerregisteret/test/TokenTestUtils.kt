package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.security.mock.oauth2.MockOAuth2Server

fun MockOAuth2Server.issueTokenXToken(
    issuerId: String = "default",
    acr: String = "idporten-loa-high",
    pid: String = TestData.fnr1
): String {
    return issueToken(
        issuerId = issuerId,
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}
