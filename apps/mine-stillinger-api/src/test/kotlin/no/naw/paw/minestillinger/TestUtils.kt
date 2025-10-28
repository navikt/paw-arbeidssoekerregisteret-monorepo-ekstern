package no.naw.paw.minestillinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.withClue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server

val MockOAuth2Server.tokenXAuthProvider: AuthProvider
    get() = AuthProvider(
        name = TokenX.name,
        audiences = listOf("default"),
        discoveryUrl = wellKnownUrl("default").toString(),
        requiredClaims = AuthProviderRequiredClaims(
            listOf("acr=Level4", "acr=idporten-loa-high"),
            true
        )
    )

fun MockOAuth2Server.sluttbrukerTokenJwt(
    id: Identitetsnummer,
    acr: String = "idporten-loa-high",
): SignedJWT =
    mapOf(
        "acr" to acr,
        "pid" to id.verdi
    ).let {
        it.plus("issuer" to TokenX.name) to issueToken(claims = it)
    }.second!!

fun MockOAuth2Server.sluttbrukerToken(
    id: Identitetsnummer,
    acr: String = "idporten-loa-high",
): String = sluttbrukerTokenJwt(id, acr).serialize()

suspend inline fun <reified T> HttpClient.get(token: SignedJWT?, path: String): Pair<HttpStatusCode, T> {
    val response = get(path) {
        token?.apply { bearerAuth(token.serialize()) }
        contentType(ContentType.Application.Json)
    }
    withClue("Response body: ${response.body<String>()}") {
        response.validateAgainstOpenApiSpec()
    }
    val body: T = response.body()
    return response.status to body
}

fun ApplicationTestBuilder.testClient(): HttpClient = createClient {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}