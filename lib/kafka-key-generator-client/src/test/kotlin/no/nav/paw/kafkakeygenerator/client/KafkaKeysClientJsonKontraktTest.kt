package no.nav.paw.kafkakeygenerator.client

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.client.factory.createObjectMapper
import no.nav.paw.kafkakeygenerator.model.IdentitetRequest
import no.nav.paw.kafkakeygenerator.model.KafkaKeysRequest

/**
 * JSON-kontrakt for request-bodyene [StandardKafkaKeysClient] sender til kafka-key-generator.
 * Klienten bygges med produksjonens [createHttpClient] (ContentNegotiation med klient-mapperen),
 * kun motoren er byttet ut med en [MockEngine] som fanger bodyen.
 *
 * Fasitfilene under `json-kontrakt/kafka-key-generator-client/` er generert fra produksjonskoden
 * og må aldri endres.
 */
class KafkaKeysClientJsonKontraktTest : FreeSpec({

    val identitetsnummer = "01017012345"

    fun fangBody(kall: suspend (StandardKafkaKeysClient) -> Unit): String {
        var body: String? = null
        val engine = MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond(
                content = """{"id":1,"key":2,"identiteter":[],"info":{"identitetsnummer":"x","lagretData":null,"pdlData":{"error":null,"id":null}}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val engineFactory = object : HttpClientEngineFactory<HttpClientEngineConfig> {
            override fun create(block: HttpClientEngineConfig.() -> Unit): HttpClientEngine = engine
        }
        val client = StandardKafkaKeysClient(
            httpClient = createHttpClient(engineFactory = engineFactory),
            baseUrl = "https://kafkakeygen",
            getAccessToken = { "token" },
        )
        runBlocking { kall(client) }
        return requireNotNull(body) { "Ingen request ble sendt" }
    }

    "hentEllerOpprett sender KafkaKeysRequest likt fasit" {
        val fasit = lesFasit("kafka-keys-request.json")
        fangBody { it.getIdAndKeyOrNull(identitetsnummer) } skalVaereLikFasit fasit
        createObjectMapper().readValue<KafkaKeysRequest>(fasit) shouldBe KafkaKeysRequest(identitetsnummer)
    }

    "info sender KafkaKeysRequest likt fasit" {
        val fasit = lesFasit("kafka-keys-request.json")
        @Suppress("DEPRECATION")
        fangBody { it.getInfo(identitetsnummer) } skalVaereLikFasit fasit
    }

    "identiteter sender IdentitetRequest likt fasit" {
        val fasit = lesFasit("identitet-request.json")
        fangBody { it.getIdentiteter(identitetsnummer, visKonflikter = true, hentFraPdl = true) } skalVaereLikFasit fasit
        createObjectMapper().readValue<IdentitetRequest>(fasit) shouldBe IdentitetRequest(identitetsnummer)
    }
})

private fun lesFasit(navn: String): String =
    requireNotNull(
        Thread.currentThread().contextClassLoader.getResource("json-kontrakt/kafka-key-generator-client/$navn")
    ) { "Fant ikke fasitfil $navn" }.readText()

private infix fun String.skalVaereLikFasit(fasit: String) {
    this shouldEqualJson {
        propertyOrder = PropertyOrder.Strict
        fieldComparison = FieldComparison.Strict
        numberFormat = NumberFormat.Strict
        arrayOrder = ArrayOrder.Strict
        typeCoercion = TypeCoercion.Disabled
        fasit
    }
}
