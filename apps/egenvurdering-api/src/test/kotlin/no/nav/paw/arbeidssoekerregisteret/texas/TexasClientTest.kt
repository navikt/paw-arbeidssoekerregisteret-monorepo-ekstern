package no.nav.paw.arbeidssoekerregisteret.texas

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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.paw.client.factory.configureJackson

class TexasClientTest : FreeSpec({
    val texasTestEndpoint = "https://texas/token"

    "Kan veksle token" - {
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe texasTestEndpoint

            respond(
                content = """{ "access_token": "vekslet_token" }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val texasClient = TexasClient(
            config = TexasClientConfig(
                endpoint = texasTestEndpoint,
                identityProvider = "tokenx",
                target = "target-app"
            ),
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            val response = texasClient.getOnBehalfOfToken(userToken = "user-token")
            response should beInstanceOf<OnBehalfOfResponse>()
            response.accessToken shouldBe "vekslet_token"
        }
    }

})

private fun testClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}

