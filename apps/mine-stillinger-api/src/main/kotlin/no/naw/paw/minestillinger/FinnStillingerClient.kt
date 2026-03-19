package no.naw.paw.minestillinger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.obo.OnBehalfOfBrukerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerByUuidListeRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.PagingResponse
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StyrkKode
import no.naw.paw.ledigestillinger.model.Tag

data class LedigeStillingerClientConfig(
    val baseUrl: String,
    val target: String
)

const val FINN_LEDIGE_STILLINGER_PATH = "/api/v1/stillinger"

class FinnStillingerClient(
    private val config: LedigeStillingerClientConfig,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient,
) {
    suspend fun finnLedigeStillinger(
        token: AccessToken,
        finnStillingerRequests: List<FinnStillingerRequest>
    ): FinnStillingerResponse {
        val newToken = texasClient.exchangeOnBehalfOfBrukerToken(
            OnBehalfOfBrukerRequest(
                userToken = token.jwt,
                target = config.target
            )
        ).accessToken

        return coroutineScope {
            val svar = finnStillingerRequests.asFlow()
                .map { request ->
                    async {
                        request.emitSpanEvent()
                        val response = httpClient.post(config.baseUrl + FINN_LEDIGE_STILLINGER_PATH) {
                            contentType(Application.Json)
                            bearerAuth(newToken)
                            setBody(request)
                        }
                        when {
                            response.status.isSuccess() -> response.body<FinnStillingerResponse>()
                            else -> throw StillingerClientException(response.status)
                        }
                    }
                }.toList()
                .awaitAll()
            val stillinger = svar.flatMap { it.stillinger }
            stillinger.emitSpanEvent()
            val nullPage: PagingResponse? = null
            val page = svar.map { it.paging }.fold(nullPage) { acc, pagingResponse ->
                acc?.copy(
                    pageSize = acc.pageSize + pagingResponse.pageSize,
                    hitSize = acc.hitSize + pagingResponse.hitSize
                )
                    ?: PagingResponse(
                        page = pagingResponse.page,
                        pageSize = pagingResponse.pageSize,
                        sortOrder = pagingResponse.sortOrder,
                        hitSize = pagingResponse.hitSize
                    )
            }
            FinnStillingerResponse(
                stillinger = stillinger,
                paging = page ?: PagingResponse()
            )
        }
    }
}

class StillingerClientException(override val status: HttpStatusCode) : ClientResponseException(
    status = status,
    message = "Feil ved henting av ledige stillinger",
    type = ErrorType.domain("stillinger").error("henting_feilet").build(),
    cause = null
)

fun <T> List<List<T>>.interleave(): List<T> {
    val maxSize = maxOfOrNull { it.size } ?: return emptyList()
    return (0 until maxSize).flatMap { index ->
        this.mapNotNull { list -> list.getOrNull(index) }
    }
}

fun FinnStillingerRequest.emitSpanEvent() {
    val type = this.type
    val builder = Attributes.builder()
    builder.put(stringKey("type"), type.name)
    val attributes = when (this) {
        is FinnStillingerByEgenskaperRequest -> {
            builder.put(stringKey("tags"), tags.joinToString(",") { it.name })
            builder.put(stringKey("paging"), this.paging.toString())
            builder.put(longKey("fylker"), fylker.size.toLong())
            builder.put(longKey("kommuner"), fylker.fold(0L) { acc, fylke -> acc + fylke.kommuner.size })
            builder.put(longKey("styrkkoder"), styrkkoder.distinct().size.toLong())
        }
        is FinnStillingerByUuidListeRequest -> {
            builder.put(longKey("antall_uuid"), this.uuidListe.size.toLong())
        }
    }
    Span.current().addEvent("finn_stillinger_request", attributes.build())
}

fun Iterable<Stilling>.emitSpanEvent() {
    val builder = Attributes.builder()
    builder.put(longKey("antall_stillinger"), this.count().toLong())
    groupBy { it.tags.map(Tag::name).sorted() }
        .forEach { (tags, stillinger) ->
            val key = if (tags.isEmpty()) "ingen_tags" else tags.joinToString("__")
            builder.put(longKey(key), stillinger.size.toLong())
        }
    val antallStyrkkoder = flatMap(Stilling::styrkkoder)
        .map(StyrkKode::kode)
        .distinct()
        .count()
    builder.put(longKey("antall_styrkkoder"), antallStyrkkoder.toLong())
    Span.current().addEvent("finn_stillinger_response", builder.build())
}

