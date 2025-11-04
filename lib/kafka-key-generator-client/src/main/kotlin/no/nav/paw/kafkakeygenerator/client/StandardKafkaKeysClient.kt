package no.nav.paw.kafkakeygenerator.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.kafkakeygenerator.model.IdentitetRequest
import no.nav.paw.kafkakeygenerator.model.IdentiteterResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysInfoResponse
import no.nav.paw.kafkakeygenerator.model.KafkaKeysRequest
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse

class StandardKafkaKeysClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val getAccessToken: () -> String,
) : KafkaKeysClient {
    override suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse? =
        httpClient.post("$baseUrl/api/v2/hentEllerOpprett") {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(Application.Json)
            setBody(KafkaKeysRequest(identitetsnummer))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<KafkaKeysResponse>()
                }

                HttpStatusCode.NotFound -> null
                else -> {
                    throw Exception("Kunne ikke hente kafka key, http_status=${response.status}, melding=${response.body<String>()}")
                }
            }
        }

    override suspend fun getIdentiteter(
        identitetsnummer: String,
        visKonflikter: Boolean,
        hentFraPdl: Boolean
    ): IdentiteterResponse =
        httpClient.post("$baseUrl/api/v2/identiteter") {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(Application.Json)
            url {
                if (hentFraPdl) parameters.append("hentPdl", "true")
                if (visKonflikter) parameters.append("visKonflikter", "true")
            }
            setBody(IdentitetRequest(identitetsnummer))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> response.body<IdentiteterResponse>()
                else -> {
                    throw Exception("Kunne ikke hente identiteter. HttpStatus=${response.status} melding=${response.body<String>()}")
                }
            }
        }

    @Deprecated("Denne vil bli fjernet", ReplaceWith("getIdentiteter(identitetsnummer)"))
    override suspend fun getInfo(identitetsnummer: String): KafkaKeysInfoResponse? {
        return httpClient.post("$baseUrl/api/v2/info") {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(Application.Json)
            setBody(KafkaKeysRequest(identitetsnummer))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<KafkaKeysInfoResponse>()
                }

                HttpStatusCode.NotFound -> null
                else -> {
                    throw Exception("Kunne ikke hente info, http_status=${response.status}, melding=${response.body<String>()}")
                }
            }
        }
    }
}