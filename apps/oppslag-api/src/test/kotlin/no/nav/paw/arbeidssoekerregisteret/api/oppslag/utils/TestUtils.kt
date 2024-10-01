package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server

fun arbeidssoekerregisterObjectMapper() =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

fun getTokenXToken(oauth: MockOAuth2Server): String {
    val tokenMap =
        mapOf(
            "acr" to "idporten-loa-high",
            "pid" to "12345678901"
        )

    val token =
        oauth.issueToken(
            claims = tokenMap
        )
    return token.serialize()
}

fun getAzureM2MToken(oauth: MockOAuth2Server): String {
    val tokenMap =
        mapOf(
            "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
            "roles" to listOf("access_as_application")
        )

    val token =
        oauth.issueToken(
            claims = tokenMap
        )
    return token.serialize()
}
