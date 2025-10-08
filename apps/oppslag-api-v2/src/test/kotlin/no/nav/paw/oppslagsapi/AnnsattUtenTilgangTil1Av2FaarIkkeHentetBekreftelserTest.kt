package no.nav.paw.oppslagsapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.configureRoutes
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.DatabaseQuerySupport
import no.nav.paw.oppslagapi.health.CompoudHealthIndicator
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Duration
import java.time.Instant
import java.util.*


class AnnsattUtenTilgangTil1Av2FaarIkkeHentetBekreftelserTest : FreeSpec({
    val tilgangsTjenesteForAnsatteMock: TilgangsTjenesteForAnsatte = mockk()
    val kafkaKeysClientMock: KafkaKeysClient = mockk()
    val databaseQuerySupportMock: DatabaseQuerySupport = mockk()
    val autorisasjonsTjeneste = AutorisasjonsTjeneste(
        tilgangsTjenesteForAnsatte = tilgangsTjenesteForAnsatteMock,
        kafkaKeysClient = kafkaKeysClientMock,
        auditLogger = AuditLogger.getLogger()
    )
    val appLogic = ApplicationQueryLogic(
        autorisasjonsTjeneste = autorisasjonsTjeneste,
        databaseQuerySupport = databaseQuerySupportMock,
        kafkaKeysClient = kafkaKeysClientMock
    )
    val startTime = Instant.now() - Duration.ofDays(30)
    val periode1 = periode(identitetsnummer = person1.first(), startet = startTime)
    val periode2 = periode(identitetsnummer = person2.first(), startet = startTime)
    val bekreftelseMelding_p1 = bekreftelseMelding(periodeId = periode1.id)
    val bekreftelseMelding_p2 = bekreftelseMelding(periodeId = periode2.id)

    every { databaseQuerySupportMock.hentRaderForPeriode(periode1.id) } returns listOf(
        Row(
            periodeId = periode1.id,
            identitetsnummer = periode1.identitetsnummer,
            timestamp = periode1.startet.tidspunkt,
            data = periode1.toOpenApi(),
            type = periode_startet_v1
        ),
        Row(
            periodeId = periode1.id,
            identitetsnummer = null,
            timestamp = startTime + Duration.ofDays(1),
            data = bekreftelseMelding_p1.toOpenApi(),
            type = bekreftelsemelding_v1
        )
    )
    every { databaseQuerySupportMock.hentRaderForPeriode(periode2.id) } returns listOf(
        Row(
            periodeId = periode2.id,
            identitetsnummer = periode2.identitetsnummer,
            timestamp = periode2.startet.tidspunkt,
            data = periode2.toOpenApi(),
            type = periode_startet_v1
        ),
        Row(
            periodeId = periode2.id,
            identitetsnummer = null,
            timestamp = startTime + Duration.ofDays(1),
            data = bekreftelseMelding_p2.toOpenApi(),
            type = bekreftelsemelding_v1
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
                            appQueryLogic = appLogic
                        )
                    }
                    val client = createClient {
                        install(ContentNegotiation) {
                            jackson {
                                registerKotlinModule()
                                registerModule(JavaTimeModule())
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            }
                        }
                    }
                    val token = oauthServer.ansattToken(
                        NavAnsatt(
                            oid = UUID.randomUUID(),
                            ident = ansatt2.verdi,
                            sikkerhetsnivaa = "tokenx:Level4"
                        )
                    )
                    val response = client.hentBekreftelser(token, listOf(periode1.id))
                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }
        }
})


