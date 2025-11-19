package no.nav.paw.oppslagapi.test

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PerioderRequest
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeListRequest
import no.nav.paw.oppslagapi.model.v3.PeriodeRequest
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun ApplicationTestBuilder.createTestHttpClient() = createClient {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

fun MockOAuth2Server.brukerToken(
    bruker: Sluttbruker,
    acr: String = "idporten-loa-high"
): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "acr" to acr,
        "pid" to bruker.ident.value
    ).let { it.plus("issuer" to TokenX.name) to issueToken(claims = it) }

fun MockOAuth2Server.ansattToken(navAnsatt: NavAnsatt): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "oid" to navAnsatt.oid,
        "NAVident" to navAnsatt.ident
    ).let { it.plus("issuer" to AzureAd.name) to issueToken(claims = it) }

fun MockOAuth2Server.anonymToken(): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "oid" to UUID.randomUUID().toString(),
        "roles" to listOf("access_as_application")
    ).let { it.plus("issuer" to AzureAd.name) to issueToken(claims = it) }

suspend fun HttpClient.hentViaGet(
    url: String,
    token: Pair<Map<String, Any>, SignedJWT>
): HttpResponse {
    val response = get(url) {
        bearerAuth(token.second.serialize())
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }
    return response
}

suspend fun HttpClient.hentViaPost(
    url: String,
    token: Pair<Map<String, Any>, SignedJWT>,
    request: Any
): HttpResponse {
    val response = post(url) {
        bearerAuth(token.second.serialize())
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(request)
    }
    return response
}

suspend fun HttpClient.hentBekreftelserV2(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    val response = hentViaPost(
        url = "/api/v2/bekreftelser",
        token = token,
        request = PerioderRequest(
            perioder = perioder
        )
    ).validateOpenApiSpec(validator = v2ApiValidator)
    return response
}

suspend fun HttpClient.hentTidslinjerV2(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    val response = hentViaPost(
        url = "/api/v2/tidslinjer",
        token = token,
        request = PerioderRequest(
            perioder = perioder
        )
    )
    return response
}

suspend fun HttpClient.hentPerioderV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/perioder",
        token = token,
        request = IdentitetsnummerRequest(
            identitetsnummer = identitetsnummer.value
        )
    ).validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentPerioderV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    periodeId: UUID
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/perioder",
        token = token,
        request = PeriodeRequest(
            periodeId = periodeId
        )
    ).validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentTidslinjerV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/tidslinjer",
        token = token,
        request = IdentitetsnummerRequest(
            identitetsnummer = identitetsnummer.value
        )
    )//.validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentTidslinjerV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/tidslinjer",
        token = token,
        request = PeriodeListRequest(
            perioder = perioder
        )
    )//.validateOpenApiSpec(validator = v3ApiValidator)
}