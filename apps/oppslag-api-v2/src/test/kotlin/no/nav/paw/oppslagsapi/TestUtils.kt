package no.nav.paw.oppslagsapi

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.toURI
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PerioderRequest
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.nio.file.Paths
import java.time.Instant
import java.util.*

private val openApiSpecPath = Paths.get("src/main/resources/openapi/openapi-spec.yaml")
val specValidator = OpenApiInteractionValidator
    .createFor(openApiSpecPath.toString())
    .build()

suspend fun HttpResponse.validatedWith(specValidator: OpenApiInteractionValidator): HttpResponse {
    val simpleResponse = SimpleResponse.Builder(
        status.value
    ).withContentType(contentType()?.let { "$it" })
        .withBody(bodyAsBytes().inputStream())
        .build()
    val resultat = no.nav.paw.oppslagsapi.specValidator.validateResponse(
        this.request.url.toURI().path,
        when (request.method) {
            io.ktor.http.HttpMethod.Get -> Request.Method.GET
            io.ktor.http.HttpMethod.Post -> Request.Method.POST
            io.ktor.http.HttpMethod.Put -> Request.Method.PUT
            io.ktor.http.HttpMethod.Delete -> Request.Method.DELETE
            io.ktor.http.HttpMethod.Patch -> Request.Method.PATCH
            else -> throw IllegalArgumentException("Ugyldig HTTP-metode for OpenAPI-validering")
        },
        simpleResponse
    )
    withClue(resultat) { resultat.hasErrors() shouldBe false }
    return this
}

fun MockOAuth2Server.personToken(
    id: Identitetsnummer,
    acr: String = "idporten-loa-high"
): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "acr" to acr,
        "pid" to id.verdi
    ).let { it.plus("issuer" to "tokenx") to issueToken(claims = it) }

fun MockOAuth2Server.ansattToken(navAnsatt: NavAnsatt): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "oid" to navAnsatt.oid,
        "NAVident" to navAnsatt.ident
    ).let { it.plus("issuer" to "azure") to issueToken(claims = it) }

fun MockOAuth2Server.anonymToken(): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "oid" to UUID.randomUUID().toString(),
        "roles" to listOf("access_as_application")
    ).let { it.plus("issuer" to "azure") to issueToken(claims = it) }

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

suspend fun HttpClient.hentBekreftelser(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    val response = post("/api/v2/bekreftelser") {
        bearerAuth(token.second.serialize())
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(
            PerioderRequest(
                perioder = perioder
            )
        )
    }
    return response
}

suspend fun HttpClient.hentTidslinjer(
    token: Pair<Map<String, Any>, SignedJWT>,
    perioder: List<UUID>
): HttpResponse {
    val response = post("/api/v2/tidslinjer") {
        bearerAuth(token.second.serialize())
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(
            PerioderRequest(
                perioder = perioder
            )
        )
    }
    return response
}

fun periode(
    periodeId: UUID = UUID.randomUUID(),
    identitetsnummer: Identitetsnummer,
    startet: Instant,
    avsluttet: Instant? = null
) = PeriodeFactory.create().build(
    id = periodeId,
    identitetsnummer = identitetsnummer.verdi,
    startet = MetadataFactory.create().build(tidspunkt = startet),
    avsluttet = avsluttet?.let { MetadataFactory.create().build(tidspunkt = it) }
)