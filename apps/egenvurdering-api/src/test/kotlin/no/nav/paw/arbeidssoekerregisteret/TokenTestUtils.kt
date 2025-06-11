package no.nav.paw.arbeidssoekerregisteret

import no.nav.security.mock.oauth2.MockOAuth2Server

fun MockOAuth2Server.issueTokenXToken(
    issuerId: String = "default",
    acr: String = "idporten-loa-high",
    pid: String = "12345678901"
): String {
    return issueToken(
        issuerId = issuerId,
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}
