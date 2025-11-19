package no.nav.paw.oppslagapi.routes.v3

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
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
import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.brukerToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.createTestHttpClient
import no.nav.paw.oppslagapi.test.hentTidslinjerV3
import java.util.*

class V3TidslinjeRouteTest : FreeSpec({
    with(TestContext()) {
        val bruker1 = TestData.bruker3
        val bruker2 = TestData.bruker4
        val bruker3 = TestData.bruker5
        val ansatt = TestData.anstatt3
        val periodeId1 = TestData.periode3_1_startet.id
        val periodeId2 = TestData.periode4_1_startet.id
        val periodeId3 = UUID.randomUUID()
        val groupedRows1: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val groupedRows2: List<Pair<UUID, List<Row<Any>>>> = TestData.rows4
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val forventetTidslinje1 = genererTidslinje(rader = groupedRows1).first().asV3()
        val forventetTidslinje2 = genererTidslinje(rader = groupedRows2).first().asV3()

        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            kafkaKeysClientMock.configureMock()
            mockOAuthServer.start()
        }
        afterSpec {
            mockOAuthServer.shutdown()
        }

        "Sluttbruker henter tidslinjer med identitetsnummer" - {
            "/api/v3/tidslinjer -> 403 Forbidden" {
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
                    val response = client.hentTidslinjerV3(
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

            "/api/v3/tidslinjer -> 200 OK - ingen perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker3.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 0
                }
            }

            "/api/v3/tidslinjer -> 200 OK - ingen aktive perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker2.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje2
                }
            }

            "/api/v3/tidslinjer -> 200 OK" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker1.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje1
                }
            }
        }

        "Sluttbruker henter tidslinjer med periodeId" - {
            "/api/v3/tidslinjer -> 403 Forbidden" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId1)
                    )
                    response.status shouldBe HttpStatusCode.Forbidden
                    val feil = response.body<ProblemDetails>()
                    feil.type shouldBe "ingen-tilgang".asSecurityErrorType()
                    feil.status shouldBe HttpStatusCode.Forbidden
                    feil.title shouldBe HttpStatusCode.Forbidden.description
                }
            }

            "/api/v3/tidslinjer -> 200 OK - ingen perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker1.ident to listOf(periodeId3 to emptyList())
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId3)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<String>()
                    println(tidslinjer)
                    /*
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 0
                     */
                }
            }

            "/api/v3/tidslinjer -> 200 OK - ingen aktive perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId2)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje2
                }
            }

            "/api/v3/tidslinjer -> 200 OK" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId1)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje1
                }
            }
        }

        "Ansatt henter tidslinjer med identitetsnummer" - {
            "/api/v3/tidslinjer -> 200 OK - ingen perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker3.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 0
                }
            }

            "/api/v3/tidslinjer -> 200 OK - ingen aktive perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker2.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje2
                }
            }

            "/api/v3/tidslinjer -> 200 OK" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        identitetsnummer = bruker1.ident
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje1
                }
            }
        }

        "Ansatt henter tidslinjer med periodeId" - {
            "/api/v3/tidslinjer -> 200 OK - ingen perioder" {
                val data: List<Pair<Identitetsnummer, List<Pair<UUID, List<Row<Any>>>>>> = listOf(
                    bruker1.ident to listOf(periodeId3 to emptyList())
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId3)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 0
                }
            }

            "/api/v3/tidslinjer -> 200 OK - ingen aktiv perioder" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId2)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje2
                }
            }

            "/api/v3/tidslinjer -> 200 OK" {
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
                    val response = client.hentTidslinjerV3(
                        token = token,
                        perioder = listOf(periodeId1)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val tidslinjer = response.body<List<Tidslinje>>()
                    tidslinjer shouldHaveSize 1
                    tidslinjer.first() shouldBe forventetTidslinje1
                }
            }
        }
    }
})