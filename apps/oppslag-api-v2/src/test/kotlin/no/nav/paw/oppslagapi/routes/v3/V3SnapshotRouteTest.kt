package no.nav.paw.oppslagapi.routes.v3

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.asSecurityErrorType
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.query.genererTidslinje
import no.nav.paw.oppslagapi.exception.PERIODE_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.oppslagapi.mapping.v3.asAggregertPeriode
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.brukerToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.createTestHttpClient
import no.nav.paw.oppslagapi.test.hentSnapshotV3
import java.util.*

class V3SnapshotRouteTest : FreeSpec({
    with(TestContext()) {
        val bruker1 = TestData.bruker3
        val bruker2 = TestData.bruker4
        val bruker3 = TestData.bruker5
        val ansatt = TestData.anstatt3
        val groupedRows1: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val groupedRows2: List<Pair<UUID, List<Row<Any>>>> = TestData.rows4
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val forventetPeriode1 = genererTidslinje(rader = groupedRows1).first().asV3().asAggregertPeriode()
        val forventetPeriode2 = genererTidslinje(rader = groupedRows2).first().asV3().asAggregertPeriode()

        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            kafkaKeysClientMock.configureMock()
            mockOAuthServer.start()
        }
        afterSpec {
            mockOAuthServer.shutdown()
        }

        "Sluttbruker henter snapshot" - {
            "/api/v3/snapshot -> 403 Forbidden" {
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
                    val token = mockOAuthServer.brukerToken(bruker = bruker1)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker3.ident
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    val feil = response.body<ProblemDetails>()
                    feil.type shouldBe "ingen-tilgang".asSecurityErrorType()
                    feil.status shouldBe HttpStatusCode.Forbidden
                    feil.title shouldBe HttpStatusCode.Forbidden.description
                }
            }

            "/api/v3/snapshot -> 404 Not Found - ingen perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker3.ident to emptyList()
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.brukerToken(bruker = bruker3)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker3.ident
                    )
                    response.status shouldBe HttpStatusCode.NotFound
                    val feil = response.body<ProblemDetails>()
                    feil.type shouldBe PERIODE_IKKE_FUNNET_ERROR_TYPE
                    feil.status shouldBe HttpStatusCode.NotFound
                    feil.title shouldBe HttpStatusCode.NotFound.description
                }
            }

            "/api/v3/snapshot -> 404 Not Found - ingen aktive perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker2.ident to groupedRows2
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.brukerToken(bruker = bruker2)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker2.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val periode = response.body<AggregertPeriode>()
                    periode shouldBe forventetPeriode2
                }
            }

            "/api/v3/snapshot -> 200 OK" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker1.ident to groupedRows1
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.brukerToken(bruker = bruker1)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker1.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val periode = response.body<AggregertPeriode>()
                    periode shouldBe forventetPeriode1
                }
            }
        }

        "Ansatt henter snapshot" - {
            "/api/v3/snapshot -> 404 Not Found - ingen perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker3.ident to emptyList()
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.ansattToken(navAnsatt = ansatt)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker3.ident
                    )
                    response.status shouldBe HttpStatusCode.NotFound
                    val feil = response.body<ProblemDetails>()
                    feil.type shouldBe PERIODE_IKKE_FUNNET_ERROR_TYPE
                    feil.status shouldBe HttpStatusCode.NotFound
                    feil.title shouldBe HttpStatusCode.NotFound.description
                }
            }

            "/api/v3/snapshot -> 404 Not Found - ingen aktive perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker2.ident to groupedRows2
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.ansattToken(navAnsatt = ansatt)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker2.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val periode = response.body<AggregertPeriode>()
                    periode shouldBe forventetPeriode2
                }
            }

            "/api/v3/snapshot -> 200 OK" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker1.ident to groupedRows1
                )
                databaseQuerySupportMock.configureMock(data)

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
                    val token = mockOAuthServer.ansattToken(navAnsatt = ansatt)
                    val response = client.hentSnapshotV3(
                        token = token,
                        identitetsnummer = bruker1.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val periode = response.body<AggregertPeriode>()
                    periode shouldBe forventetPeriode1
                }
            }
        }
    }
})