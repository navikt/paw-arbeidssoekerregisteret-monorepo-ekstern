package no.nav.paw.aareg.test

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.paw.aareg.client.AaregClient
import no.nav.paw.client.factory.createHttpClient

object MockResponse {
    val arbeidsforhold = "aareg-arbeidsforhold.json".readResource()
    val error = "error.json".readResource()
}

fun mockAaregClient(content: String, statusCode: HttpStatusCode = HttpStatusCode.OK): AaregClient {
    val mockEngine = MockEngine {
        respond(
            content = content,
            status = statusCode,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }

    val mockHttpClient = createHttpClient(mockEngine)
    return AaregClient("url", httpClient = mockHttpClient) { "fake token" }
}
