package no.nav.paw.arbeidssoekerregisteret.clients

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.config.TexasClientConfig

class TexasClientTest : FreeSpec({

    "Kan veksle token" - {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{ "access_token": "token" }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val testClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        }

        val texasClient = TexasClient(
            config = TexasClientConfig(
                endpoint = "https://fake-texas/token",
                identityProvider = "tokenx",
                target = "target-app"
            ),
            httpClient = testClient
        )

        runBlocking {
            val response = texasClient.getOnBehalfOfToken("user-token")
            response should beInstanceOf<OnBehalfOfResponse>()
            response.accessToken shouldBe "token"
        }
    }

})

