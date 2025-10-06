package no.nav.paw.arbeidssoekerregisteret.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.m2m.MachineToMachineTokenResponse

class IdentitetClientTest : FreeSpec({

    val baseUrl = "https://identitet"
    val m2mToken = "m2m-token"
    val texasClient = mockk<TexasClient>()
    coEvery { texasClient.getMachineToMachineToken() } returns MachineToMachineTokenResponse(accessToken = m2mToken)

    "Hent identer for gitt ident" {
        val ident1 = "11111111111"
        val ident2 = "22222222222"
        val forventetResponse = IdentitetResponse(identiteter = listOf(ident1, ident2))
        //language=JSON
        val identiteterResponse = """{"identiteter":["$ident1","$ident2"]}"""
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe "$baseUrl$identiteterPath"
            request.headers[Authorization] shouldBe "Bearer $m2mToken"

            respond(
                content = identiteterResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val identitetClient = IdentitetClient(
            config = IdentitetClientConfig(baseUrl),
            texasClient = texasClient,
            testClient(mockEngine)
        )

        runBlocking {
            val response = identitetClient.hentIdentiteter(
                request = IdentitetRequest(identitet = "10987654321")
            )
            response.identiteter shouldContainExactly forventetResponse.identiteter
        }
    }

    "Tar med query params og traceparent header" {
        val traceparent = "traceparent"
        val identitet = "10987654321"
        //language=JSON
        val identiteterResponse = """{"identiteter":["$identitet"]}"""
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.encodedPath shouldBe identiteterPath
            request.url.parameters["hentPdl"] shouldBe "true"
            request.url.parameters["visKonflikter"] shouldBe "true"
            request.headers["traceparent"] shouldBe traceparent
            request.headers[Authorization] shouldBe "Bearer $m2mToken"

            respond(
                content = identiteterResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val client = IdentitetClient(
            config = IdentitetClientConfig(baseUrl),
            texasClient = texasClient,
            testClient(mockEngine)
        )

        runBlocking {
            val response = client.hentIdentiteter(
                request = IdentitetRequest(identitet = identitet),
                visKonflikter = true,
                hentPdl = true,
                traceparent
            )
            response.identiteter shouldContainExactly listOf(identitet)
        }
    }

    "IdentitetClientException og problemDetails" {
        val mockEngine = MockEngine {
            respond(
                content = //language=JSON
                    """
                    {
                      "id":"3cd944fb-6187-41a8-91b2-b172f2baf890",
                      "type":"urn:paw:http:kunne-ikke-tolke-forespoersel",
                      "status":400,
                      "title":"Bad Request",
                      "detail":"Kunne ikke tolke forespørsel",
                      "instance":"/api/v2/identiteter",
                      "timestamp":"2021-01-01T12:00:00.000Z"
                    }
                    """.trimIndent(),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val client = IdentitetClient(
            config = IdentitetClientConfig(baseUrl),
            texasClient = texasClient,
            testClient(mockEngine)
        )


        runBlocking {
            val exception = shouldThrow<IdentitetClientException> {
                client.hentIdentiteter(IdentitetRequest("ugyldig"))
            }
            exception.message shouldContain "Statuskode: 400"
            exception.message shouldContain "Bad Request"
            exception.message shouldContain "Kunne ikke tolke forespørsel"
        }
    }

    "IdentitetClientException uten problemDetails" {
        val mockEngine = MockEngine {
            respond(
                content = """{"oops":"serveren smeltet"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val client = IdentitetClient(
            config = IdentitetClientConfig(baseUrl),
            texasClient = texasClient,
            testClient(mockEngine)
        )

        runBlocking {
            val exception = shouldThrow<IdentitetClientException> {
                client.hentIdentiteter(IdentitetRequest("hugga bugga"))
            }
            exception.message shouldContain "Statuskode: 500"
        }
    }
})

private fun testClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
