package no.nav.paw.oppslagapi.test

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
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringRequest
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.model.v2.PerioderRequest
import no.nav.paw.oppslagapi.model.v3.IdentitetsnummerQueryRequest
import no.nav.paw.oppslagapi.model.v3.PerioderQueryRequest
import no.nav.paw.oppslagapi.utils.configureJacksonForV3
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun ApplicationTestBuilder.createTestHttpClient() = createClient {
    install(ContentNegotiation) {
        jackson {
            configureJacksonForV3()
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

suspend fun HttpClient.hentPerioderV1(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v1/veileder/arbeidssoekerperioder",
        token = token,
        request = ArbeidssoekerperiodeRequest(
            identitetsnummer = identitetsnummer.value
        )
    )//.validateOpenApiSpec(validator = v1ApiValidator)
}

suspend fun HttpClient.hentProfileringV1(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer,
    periodeId: UUID
): HttpResponse {
    return hentViaPost(
        url = "/api/v1/veileder/profilering",
        token = token,
        request = ProfileringRequest(
            identitetsnummer = identitetsnummer.value,
            periodeId = periodeId
        )
    )//.validateOpenApiSpec(validator = v1ApiValidator)
}

suspend fun HttpClient.hentAggregertePerioderV1(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v1/veileder/arbeidssoekerperioder-aggregert",
        token = token,
        request = ArbeidssoekerperiodeRequest(
            identitetsnummer = identitetsnummer.value
        )
    )//.validateOpenApiSpec(validator = v1ApiValidator)
}

suspend fun HttpClient.hentBekreftelserV2(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    return hentViaPost(
        url = "/api/v2/bekreftelser",
        token = token,
        request = PerioderRequest(
            perioder = perioder
        )
    ).validateOpenApiSpec(validator = v2ApiValidator)
}

suspend fun HttpClient.hentTidslinjerV2(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    return hentViaPost(
        url = "/api/v2/tidslinjer",
        token = token,
        request = PerioderRequest(
            perioder = perioder
        )
    )//.validateOpenApiSpec(validator = v2ApiValidator)
}

suspend fun HttpClient.hentSnapshotV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/snapshot",
        token = token,
        request = IdentitetsnummerQueryRequest(
            identitetsnummer = identitetsnummer.value
        )
    ).validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentPerioderV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/perioder",
        token = token,
        request = IdentitetsnummerQueryRequest(
            identitetsnummer = identitetsnummer.value
        )
    ).validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentPerioderV3(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    return hentViaPost(
        url = "/api/v3/perioder",
        token = token,
        request = PerioderQueryRequest(
            perioder = perioder
        )
    ).validateOpenApiSpec(validator = v3ApiValidator)
}

suspend fun HttpClient.hentSnapshotV4(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v4/snapshot",
        token = token,
        request = IdentitetsnummerQueryRequest(
            identitetsnummer = identitetsnummer.value
        )
    ).validateOpenApiSpec(validator = v4ApiValidator)
}

suspend fun HttpClient.hentPerioderV4(
    token: Pair<Map<String, Any>, SignedJWT>,
    identitetsnummer: Identitetsnummer
): HttpResponse {
    return hentViaPost(
        url = "/api/v4/perioder",
        token = token,
        request = IdentitetsnummerQueryRequest(
            identitetsnummer = identitetsnummer.value
        )
    ).validateOpenApiSpec(validator = v4ApiValidator)
}

suspend fun HttpClient.hentPerioderV4(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    return hentViaPost(
        url = "/api/v4/perioder",
        token = token,
        request = PerioderQueryRequest(
            perioder = perioder
        )
    ).validateOpenApiSpec(validator = v4ApiValidator)
}