package no.naw.paw.minestillinger.brukerprofil.direktemeldte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.opentelemetry.api.trace.Span
import no.nav.paw.felles.model.Identitetsnummer
import no.naw.paw.minestillinger.appLogger

interface DirekteMeldteStillinger {
    suspend fun skalSeDirektemeldteStillinger(identitetsnummer: Identitetsnummer): Boolean
}

data class DirekteMeldteStillingerRequest(
    val identnummer: String
)

data class DirekteMeldteStillingerResponse(
    val harTilgangTilDirektemeldteStillinger: Boolean
)

class DirekteMeldteStillingerHttpClient(
    private val httpClient: HttpClient,
    private val getM2MToken: (String) -> String,
    val config: DirekteMeldteStillingerConfig,
): DirekteMeldteStillinger {
    override suspend fun skalSeDirektemeldteStillinger(identitetsnummer: Identitetsnummer): Boolean {
        val token = getM2MToken(config.scope)
        return try {
            val response = httpClient.post(config.url) {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(DirekteMeldteStillingerRequest(identnummer = identitetsnummer.value))
            }
            if (response.status == io.ktor.http.HttpStatusCode.OK) {
                response.body<DirekteMeldteStillingerResponse>()
                    .harTilgangTilDirektemeldteStillinger
            } else {
                false
            }
        } catch (e: Exception) {
            appLogger.error("Feil ved avklaring om person skal se direktemeldte stillinger", e)
            Span.current().recordException(e)
            false
        }
    }
}

class DirekteMeldteStillingerSkalAldriVises: DirekteMeldteStillinger {
    override suspend fun skalSeDirektemeldteStillinger(identitetsnummer: Identitetsnummer): Boolean {
        return false
    }
}
