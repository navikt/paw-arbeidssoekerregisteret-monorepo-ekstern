package no.nav.paw.oppslagapi.routes.v2

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.configureRoutes
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.health.CompoudHealthIndicator
import no.nav.paw.oppslagapi.model.v2.TidslinjeResponse
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.anonymToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.createTestHttpClient
import no.nav.paw.oppslagapi.test.hentTidslinjerV2
import no.nav.paw.oppslagapi.test.testLogger
import no.nav.paw.oppslagapi.utils.objectMapper
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Duration
import java.time.Instant


class AnonymM2MFaarHentetTidslinjerTest : FreeSpec({
    with(TestContext()) {
        val startTime = Instant.now() - Duration.ofDays(30)
        val periode1 = TestData.periode(identitetsnummer = TestData.fnr1, startet = startTime)
        val periode1Avsluttet = TestData.periode(
            identitetsnummer = Identitetsnummer(periode1.identitetsnummer),
            startet = periode1.startet.tidspunkt,
            avsluttet = periode1.startet.tidspunkt + Duration.ofDays(10)
        )
        val bekreftelseMelding = bekreftelseMelding(periodeId = periode1.id)

        every { databaseQuerySupportMock.hentRaderForPeriode(periode1.id) } returns listOf(
            Row(
                periodeId = periode1.id,
                identitetsnummer = periode1.identitetsnummer,
                timestamp = periode1.startet.tidspunkt,
                data = periode1.startet.toOpenApi(),
                type = periode_startet_v1
            ),
            Row(
                periodeId = periode1.id,
                identitetsnummer = null,
                timestamp = startTime + Duration.ofDays(1),
                data = bekreftelseMelding.toOpenApi(),
                type = bekreftelsemelding_v1
            ),
            Row(
                periodeId = periode1.id,
                identitetsnummer = periode1.identitetsnummer,
                timestamp = periode1Avsluttet.avsluttet.tidspunkt,
                data = periode1Avsluttet.avsluttet.toOpenApi(),
                type = periode_avsluttet_v1
            )
        )
        tilgangsTjenesteForAnsatteMock.configureMock()
        val oauthServer = MockOAuth2Server()
        beforeSpec {
            oauthServer.start()
        }
        afterSpec {
            oauthServer.shutdown()
        }
        "Verifiser at endepunkter fungerer" - {
            "/api/v2/bekreftelser" {
                testApplication {
                    application {
                        configureKtorServer(
                            prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                            meterBinders = emptyList(),
                            authProviders = oauthServer.createAuthProviders()
                        )
                    }
                    routing {
                        configureRoutes(
                            healthIndicator = CompoudHealthIndicator(),
                            prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                            queryLogic = mockedQueryLogic
                        )
                    }
                    val client = createTestHttpClient()
                    val token = oauthServer.anonymToken()
                    val response = client.hentTidslinjerV2(token, listOf(periode1.id))
                    response.status shouldBe HttpStatusCode.OK
                    val body: TidslinjeResponse = response.body()
                    testLogger.info(objectMapper.writeValueAsString(body))
                    testLogger.info("Hentet tidslinjer: $body")
                }
            }
        }
    }
})


