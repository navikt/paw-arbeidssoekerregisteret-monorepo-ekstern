package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.issueTokenXToken(
    acr: String = "idporten-loa-high",
    pid: String = TestData.fnr1
): String {
    return issueToken(
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureToken(
    oid: UUID = UUID.randomUUID(),
    name: String = "Kari Nordmann",
    navIdent: String = TestData.navIdent1
): String {
    return issueToken(
        claims = mapOf(
            "oid" to oid.toString(),
            "name" to name,
            "NAVident" to navIdent
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureM2MToken(
    oid: UUID = UUID.randomUUID(),
    roles: List<String> = listOf("access_as_application"),
): String {
    return issueToken(
        claims = mapOf(
            "oid" to oid.toString(),
            "roles" to roles
        )
    ).serialize()
}
