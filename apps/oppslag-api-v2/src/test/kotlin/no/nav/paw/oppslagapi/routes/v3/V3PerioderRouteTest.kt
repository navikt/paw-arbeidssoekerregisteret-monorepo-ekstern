package no.nav.paw.oppslagapi.routes.v3

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.createTestHttpClient
import no.nav.paw.oppslagapi.test.hentPerioderV3
import java.util.*

class V3PerioderRouteTest : FreeSpec({
    with(TestContext()) {
        beforeSpec {
            mockOAuthServer.start()
        }
        afterSpec {
            mockOAuthServer.shutdown()
        }

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
                        queryLogic = mockedQueryLogic
                    )
                }
                val client = createTestHttpClient()
                val token = mockOAuthServer.ansattToken(navAnsatt = TestData.navAnstatt1)
                val response = client.hentPerioderV3(
                    token = token,
                    periodeId = UUID.randomUUID()
                )
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})