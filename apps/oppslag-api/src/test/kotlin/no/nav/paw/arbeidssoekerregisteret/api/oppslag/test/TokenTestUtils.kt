package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import no.nav.security.mock.oauth2.MockOAuth2Server

fun MockOAuth2Server.issueTokenXToken(): String {
    return issueToken(
        claims = mapOf(
            "acr" to "idporten-loa-high",
            "pid" to "12345678901"
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureToken(): String {
    return issueToken(
        claims = mapOf(
            "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
            "name" to "Kari Nordmann",
            "NAVident" to "NO12345"
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureM2MToken(): String {
    return issueToken(
        claims = mapOf(
            "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
            "roles" to listOf("access_as_application")
        )
    ).serialize()
}
