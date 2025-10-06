package no.nav.paw.kafkakeygenerator.client

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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.paw.client.factory.configureJackson

class KafkaKeysClientTest : FreeSpec({

    val baseUrl = "https://kafkakeygen"
    val identiteterUrl = "$baseUrl/api/v2/identiteter"
    val infoUrl = "$baseUrl/api/v2/info"
    val hentEllerOpprettUrl = "$baseUrl/api/v2/hentEllerOpprett"
    val testToken = "token"

    "Hent identer fra /api/v2/identiteter og returnerer IdentitetResponse" {
        val ident1 = "11111111111"
        val ident2 = "22222222222"
        val ident3 = "33333333333"
        //language=JSON
        val responseJson = """
          {
            "arbeidssoekerId": 10170,
            "recordKey": 170,
            "identiteter": [
              {"identitet": $ident1, "type": "AKTORID", "gjeldende": true},
              {"identitet": $ident2, "type": "ARBEIDSSOEKERID", "gjeldende": true},
              {"identitet": $ident3, "type": "FOLKEREGISTERIDENT", "gjeldende": true}
            ],
            "pdlIdentiteter": [
              {"identitet": $ident3, "type": "FOLKEREGISTERIDENT", "gjeldende": true}
            ],
            "konflikter": [
              {
                "type": "MERGE",
                "detaljer": {
                  "aktorIdListe": ["1001", "1002"],
                  "arbeidssoekerIdListe": [101, 102]
                }
              }
            ]
          }""".trimIndent()

        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe identiteterUrl
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $testToken"

            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val kafkaKeysClient = StandardKafkaKeysClient(
            httpClient = testClient(mockEngine),
            kafkaKeysUrl = hentEllerOpprettUrl,
            kafkaKeysInfoUrl = infoUrl,
            kafkaKeysIdentiteterUrl = identiteterUrl,
            getAccessToken = { testToken }
        )

        runBlocking {
            val response = kafkaKeysClient.getIdentiteter(
                identitetsnummer = ident3
            )
            response.arbeidssoekerId shouldBe 10170L
            response.recordKey shouldBe 170L
            response.identiteter.map { it.identitet } shouldContainExactly listOf(ident1, ident2, ident3)
            response.pdlIdentiteter?.single()?.type shouldBe IdentitetType.FOLKEREGISTERIDENT

            response.konflikter.size shouldBe 1
            val konflikt = response.konflikter.single()
            konflikt.type shouldBe KonfliktType.MERGE
            konflikt.detaljer!!.aktorIdListe shouldContainExactly listOf("1001", "1002")
            konflikt.detaljer.arbeidssoekerIdListe shouldContainExactly listOf(101L, 102L)
        }
    }

    "Tar med query params og traceparent header" {
        val traceparent = "traceparent"
        val engine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.parameters["visKonflikter"] shouldBe "true"
            request.url.parameters["hentPdl"] shouldBe "true"
            request.headers["traceparent"] shouldBe traceparent
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $testToken"

            //language=JSON
            val responseJson = """{ "identiteter": [], "konflikter": [] }"""
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val client = StandardKafkaKeysClient(
            httpClient = testClient(engine),
            kafkaKeysUrl = hentEllerOpprettUrl,
            kafkaKeysInfoUrl = infoUrl,
            kafkaKeysIdentiteterUrl = identiteterUrl,
            getAccessToken = { testToken }
        )

        runBlocking {
            val response = client.getIdentiteter(
                identitetsnummer = "01017012345",
                visKonflikter = true,
                hentPdl = true,
                traceparent = traceparent
            )
            response.identiteter.shouldBe(emptyList())
            response.konflikter.shouldBe(emptyList())
        }
    }

    "Håndterer ProblemDetails" {
        //language=JSON
        val problem = """
          {
            "id":"eea6958a-273d-4dbc-9c85-faaa37530cbf",
            "type":"urn:paw:http:kunne-ikke-tolke-forespoersel",
            "status":400,
            "title":"Bad Request",
            "detail":"Kunne ikke tolke forespørsel",
            "instance":"/api/v2/identiteter",
            "timestamp":"2021-01-01T12:00:00.000Z"
          }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = problem,
                status = HttpStatusCode.BadRequest,
                headers = headersOf(ContentType, Application.Json.toString())
            )
        }

        val kafkaKeysClient = StandardKafkaKeysClient(
            httpClient = testClient(engine),
            kafkaKeysUrl = hentEllerOpprettUrl,
            kafkaKeysInfoUrl = infoUrl,
            kafkaKeysIdentiteterUrl = identiteterUrl,
            getAccessToken = { "token" }
        )

        runBlocking {
            val exception = shouldThrow<Exception> {
                kafkaKeysClient.getIdentiteter("ugyldig")
            }
            exception.message shouldContain "HttpStatus=400"
            exception.message shouldContain "Bad Request"
            exception.message shouldContain "Kunne ikke tolke forespørsel"
        }
    }
})

private fun testClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        jackson { configureJackson() }
    }
}
