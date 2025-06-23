package no.nav.paw.client.api.oppslag.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.client.api.oppslag.exception.ArbeidssoekerperioderAggregertOppslagResponseException
import no.nav.paw.client.api.oppslag.exception.EgenvurderingOppslagResponseException
import no.nav.paw.client.api.oppslag.exception.PerioderOppslagResponseException
import no.nav.paw.client.api.oppslag.exception.ProfileringOppslagResponseException
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.client.api.oppslag.models.EgenvurderingResponse
import no.nav.paw.client.api.oppslag.models.ProfileringResponse
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.logging.logger.buildNamedLogger

class ApiOppslagClient(
    private val baseUrl: String,
) {
    private val logger = buildNamedLogger("http.periode")
    private val httpClient: HttpClient = createHttpClient()

    suspend fun findPerioder(identitet: String, tokenProvider: () -> String): List<ArbeidssoekerperiodeResponse> {
        logger.debug("Henter arbeidssoekerperioder fra API Oppslag")
        val response = httpClient.post("$baseUrl/api/v1/arbeidssoekerperioder") {
            bearerAuth(tokenProvider.invoke())
            contentType(ContentType.Application.Json)
            setBody(ArbeidssoekerperiodeRequest(identitet))
        }
        return response.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<List<ArbeidssoekerperiodeResponse>>()
                else -> {
                    val body = it.body<String>()
                    throw PerioderOppslagResponseException(it.status, "Henting av perioder feilet. body=$body")
                }
            }
        }
    }

    suspend fun findSisteArbeidssoekerperioderAggregert(tokenProvider: () -> String): List<ArbeidssoekerperiodeAggregertResponse> {
        logger.debug("Henter arbeidssoekerperioderAggregert fra API Oppslag")
        val response = httpClient.get("$baseUrl/api/v1/arbeidssoekerperioder-aggregert?siste=true") {
            bearerAuth(tokenProvider.invoke())
        }
        return response.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<List<ArbeidssoekerperiodeAggregertResponse>>()
                else -> {
                    val body = it.body<String>()
                    throw ArbeidssoekerperioderAggregertOppslagResponseException(
                        it.status,
                        "Henting av arbeidssoekerperioderAggregert feilet. body=$body"
                    )
                }
            }
        }
    }

    suspend fun findProfilering(tokenProvider: () -> String): List<ProfileringResponse> {
        logger.debug("Henter profilering fra API Oppslag")
        val response = httpClient.get("$baseUrl/api/v1/profilering/") {
            bearerAuth(tokenProvider.invoke())
        }
        return response.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<List< ProfileringResponse>>()
                else -> {
                    val body = it.body<String>()
                    throw ProfileringOppslagResponseException(
                        it.status,
                        "Henting av profilering feilet. body=$body"
                    )
                }
            }
        }
    }

    suspend fun findEgenvurdering(tokenProvider: () -> String): List<EgenvurderingResponse> {
        logger.debug("Henter egenvurdering fra API Oppslag")
        val response = httpClient.get("$baseUrl/api/v1/profilering/egenvurdering") {
            bearerAuth(tokenProvider.invoke())
        }
        return response.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<List<EgenvurderingResponse>>()
                else -> {
                    val body = it.body<String>()
                    throw EgenvurderingOppslagResponseException(
                        it.status,
                        "Henting av egenvurdering feilet. body=$body"
                    )
                }
            }
        }
    }
}