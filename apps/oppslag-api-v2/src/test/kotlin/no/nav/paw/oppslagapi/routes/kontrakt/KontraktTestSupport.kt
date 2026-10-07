package no.nav.paw.oppslagapi.routes.kontrakt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.HttpClient
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.routes.v1.v1Routes
import no.nav.paw.oppslagapi.routes.v2.v2Routes
import no.nav.paw.oppslagapi.routes.v3.v3Routes
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.createTestHttpClient
import java.time.Instant
import java.util.*

/**
 * Felles støtte for JSON-kontrakttestene (fasitfiler) for HTTP-responsene i oppslag-api-v2.
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/api/` er generert én gang fra produksjonskoden
 * og må aldri endres. De låser formatet vi sender til konsumenter. Feiler en kontrakttest, er det
 * produksjonskoden som har endret formatet, og endringen må vurderes som en kontraktsendring.
 */
private const val FASIT_ROT = "/json-kontrakt/api"

/**
 * ProblemDetails har tilfeldig `id` og `tidspunkt` (`timestamp`). Før sammenligning verifiserer vi at
 * feltene finnes og har riktig format (UUID og ISO-8601-instant), og erstatter verdiene med disse
 * faste verdiene. Plasseringen av feltene i objektet beholdes, så feltrekkefølgen sjekkes fortsatt strengt.
 */
const val FAST_PROBLEM_ID = "00000000-0000-0000-0000-000000000000"
const val FAST_PROBLEM_TIMESTAMP = "1970-01-01T00:00:00Z"

fun lesFasit(sti: String): String =
    (KontraktTestData::class.java.getResource("$FASIT_ROT/$sti")
        ?: error("Fant ikke fasitfil $FASIT_ROT/$sti")).readText()

/**
 * Sammenligner med fasitfilen med strengeste innstillinger: feltrekkefølge, felter, tallformat og
 * rekkefølge i lister må være identiske, og ingen typekonvertering er tillatt.
 */
fun String.skalVaereLikFasit(sti: String) {
    val fasit = lesFasit(sti)
    this.shouldEqualJson {
        propertyOrder = PropertyOrder.Strict
        fieldComparison = FieldComparison.Strict
        numberFormat = NumberFormat.Strict
        arrayOrder = ArrayOrder.Strict
        typeCoercion = TypeCoercion.Disabled
        fasit
    }
}

/**
 * Bytter ut tilfeldig `id` og `timestamp` i en ProblemDetails-body med faste verdier, etter å ha
 * verifisert formatet. Returnerer JSON med samme feltrekkefølge som originalen.
 */
fun normaliserProblemDetails(body: String): String {
    val node = ObjectMapper().readTree(body) as ObjectNode
    val id = node.get("id").shouldNotBeNull()
    UUID.fromString(id.textValue().shouldNotBeNull())
    val timestamp = node.get("timestamp").shouldNotBeNull()
    Instant.parse(timestamp.textValue().shouldNotBeNull())
    node.put("id", FAST_PROBLEM_ID)
    node.put("timestamp", FAST_PROBLEM_TIMESTAMP)
    return node.toString()
}

/**
 * Starter de ekte v1-, v2- og v3-rutene med ekte ContentNegotiation og feilhåndtering,
 * med mocket databaselag som returnerer [KontraktTestData].
 */
fun TestContext.kontraktTestApplication(
    perioder: List<UUID> = KontraktTestData.standardPerioder,
    block: suspend ApplicationTestBuilder.(HttpClient) -> Unit
) {
    every { databaseQuerySupportMock.hentPerioder(any()) } returns emptyList()
    KontraktTestData.raderPerPeriode.forEach { (periodeId, rader) ->
        every { databaseQuerySupportMock.hentRaderForPeriode(periodeId) } returns rader
    }
    every { databaseQuerySupportMock.hentPerioder(KontraktTestData.identitetsnummer) } returns perioder
    testApplication {
        application {
            configureKtorServer(
                prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                meterBinders = emptyList(),
                authProviders = mockOAuthServer.createAuthProviders()
            )
        }
        routing {
            v1Routes(queryLogic = mockedQueryLogic)
            v2Routes(queryLogic = mockedQueryLogic)
            v3Routes(queryLogic = mockedQueryLogic)
        }
        block(createTestHttpClient())
    }
}
