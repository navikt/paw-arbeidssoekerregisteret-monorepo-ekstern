package no.nav.paw.oppslagsapi

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Instant
import java.util.*
import kotlin.collections.plus

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
            ApiV2BekreftelserPostRequest(
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
            ApiV2BekreftelserPostRequest(
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