package no.nav.paw.aareg.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import no.nav.paw.aareg.model.Result

/**
 * klient for å hente ut aktive arbeidsforhold på en person
 */


class AaregClient(
    private val url: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String
) {
    private val sikkerLogger: Logger = LoggerFactory.getLogger("tjenestekall")
    private val logger: Logger = LoggerFactory.getLogger("paw-aareg-client")

    suspend fun hentArbeidsforhold(ident: String, callId: String): Result  {
        try {
            val response = httpClient.get(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(getAccessToken())
                header("Nav-Call-Id", callId)
                header("Nav-Personident", ident)
            }.also {
                logger.info("Hentet arbeidsforhold fra aareg med status=${it.status}")
                sikkerLogger.debug("Svar fra aareg-API: " + it.bodyAsText())
            }

            return if(response.status.isSuccess()) {
                Result.Success(response.body())
            } else {
                logger.error("Hente arbeidsforhold callId=[$callId] feilet med http-kode ${response.status}")
                Result.Failure(response.body())
            }
        } catch (responseException: ResponseException) {
            logger.error("Hente arbeidsforhold callId=[$callId] feilet med http-kode ${responseException.response.status}")
            throw responseException
        } catch (connectException: ConnectException) {
            logger.error("Hente arbeidsforhold callId=[$callId] feilet:", connectException)
            throw connectException
        } catch (jsonConvertException: JsonConvertException) {
            logger.error("Hente arbeidsforhold callId=[$callId] feilet, kunne ikke lese JSON")
            sikkerLogger.error("Hente arbeidsforhold callId=[$callId] feilet", jsonConvertException)
            throw jsonConvertException
        }
    }
}

