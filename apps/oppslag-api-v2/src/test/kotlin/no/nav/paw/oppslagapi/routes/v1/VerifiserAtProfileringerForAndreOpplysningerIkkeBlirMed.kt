package no.nav.paw.oppslagapi.routes.v1

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.ansattToken
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.oppslagapi.test.createAuthProviders
import no.nav.paw.oppslagapi.test.hentAggregertePerioderV1
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Duration
import java.time.Instant
import java.util.*


class VerifiserAtProfileringerForAndreOpplysningerIkkeBlirMed : FreeSpec({
    with(TestContext()) {
        val startTime = Instant.now() - Duration.ofDays(30)
        val periode1 = TestData.periode(identitetsnummer = TestData.fnr1, startet = startTime)
        val bekreftelseMelding = bekreftelseMelding(periodeId = periode1.id)
        val opplysningerId = UUID.randomUUID()
        every { databaseQuerySupportMock.hentPerioder(Identitetsnummer(periode1.identitetsnummer)) } returns listOf(
            periode1.id
        )
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
                identitetsnummer = null,
                timestamp = periode1.startet.tidspunkt,
                data = createOpplysninger(id = opplysningerId, periodeId = periode1.id).toOpenApi(),
                type = opplysninger_om_arbeidssoeker_v4
            ),
            Row(
                periodeId = periode1.id,
                identitetsnummer = null,
                timestamp = periode1.startet.tidspunkt,
                data = createProfilering(periodeId = periode1.id, opplysningerId = UUID.randomUUID()).toOpenApi(),
                type = profilering_v1
            ),
            Row(
                periodeId = periode1.id,
                identitetsnummer = null,
                timestamp = periode1.startet.tidspunkt,
                data = createProfilering(periodeId = periode1.id, opplysningerId = UUID.randomUUID()).toOpenApi(),
                type = profilering_v1
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
        "Verifiser at profileringer for andre opplysninger ikke blir med" - {
            "/api/v1/veileder/arbeidssoekerperioder-aggregert" {
                testApplication {
                    application {
                        configureKtorServer(
                            prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                            meterBinders = emptyList(),
                            authProviders = oauthServer.createAuthProviders()
                        )
                    }
                    routing {
                        v1Routes(
                            queryLogic = mockedQueryLogic
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
                    val token = oauthServer.ansattToken(navAnsatt = TestData.anstatt1)
                    val response = client.hentAggregertePerioderV1(
                        token = token,
                        identitetsnummer = Identitetsnummer(periode1.identitetsnummer)
                    )
                    response.status shouldBe HttpStatusCode.OK
                    val body: List<ArbeidssoekerperiodeAggregertResponse> = response.body()
                    body.size shouldBe 1
                    val data = body.first()
                    data.opplysningerOmArbeidssoeker?.size shouldBe 1
                    data.opplysningerOmArbeidssoeker?.firstOrNull()?.profilering shouldBe null
                }
            }
        }
    }
})


