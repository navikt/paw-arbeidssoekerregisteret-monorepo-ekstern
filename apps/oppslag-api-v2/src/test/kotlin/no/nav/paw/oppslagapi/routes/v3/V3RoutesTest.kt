package no.nav.paw.oppslagapi.routes.v3

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.model.v3.PeriodeRequest
import no.nav.paw.oppslagapi.model.v3.QueryType
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestDataV3
import no.nav.paw.oppslagapi.test.createTestHttpClient
import no.nav.paw.oppslagapi.test.v3ApiValidator
import no.nav.paw.oppslagapi.test.validateOpenApiSpec
import no.nav.paw.oppslagsapi.ansattToken
import no.nav.paw.oppslagsapi.createAuthProviders
import no.nav.paw.oppslagsapi.hentViaPost
import java.util.*

class V3RoutesTest : FreeSpec({
    with(TestContext()) {
        beforeSpec {
            mockOAuthServer.start()
        }
        afterSpec {
            mockOAuthServer.shutdown()
        }

        "Test suite for aggregerte perioder" - {
            "/api/v3/perioder" {
                testApplication {
                    application {
                        configureKtorServer(
                            prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                            meterBinders = emptyList(),
                            authProviders = mockOAuthServer.createAuthProviders()
                        )
                    }
                    routing {
                        v3Routes(
                            appQueryLogic = mockedQueryLogic
                        )
                    }
                    val client = createTestHttpClient()
                    val token = mockOAuthServer.ansattToken(navAnsatt = TestDataV3.navAnstatt1)
                    val response = client.hentViaPost(
                        url = "/api/v3/perioder",
                        token = token,
                        request = PeriodeRequest(
                            type = QueryType.PERIODE_ID,
                            periodeId = UUID.randomUUID()
                        )
                    )
                    response.validateOpenApiSpec(validator = v3ApiValidator)
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    }
})