package no.nav.paw.v1Api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringRequest
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.configureKtorServer
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.DatabaseQeurySupport
import no.nav.paw.oppslagapi.routes.V1_API_BASE_PATH
import no.nav.paw.oppslagapi.routes.V1_API_VEILEDER_ARBEIDSSOEKERPERIODER
import no.nav.paw.oppslagapi.routes.V1_API_VEILEDER_ARBEIDSSOEKERPERIODER_AGGREGERT
import no.nav.paw.oppslagapi.routes.V1_API_VEILEDER_PROFILERING
import no.nav.paw.oppslagapi.routes.v1Routes
import no.nav.paw.oppslagsapi.ansatt1
import no.nav.paw.oppslagsapi.ansattToken
import no.nav.paw.oppslagsapi.configureMock
import no.nav.paw.oppslagsapi.createAuthProviders
import no.nav.paw.oppslagsapi.hentViaPost
import no.nav.paw.oppslagsapi.periode
import no.nav.paw.oppslagsapi.person1
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Duration
import java.time.Instant
import java.util.*


class VerifiserV1Endepunkter : FreeSpec({
    val tilgangsTjenesteForAnsatteMock: TilgangsTjenesteForAnsatte = mockk()
    val kafkaKeysClientMock: KafkaKeysClient = inMemoryKafkaKeysMock()
    val databaseQuerySupportMock: DatabaseQeurySupport = mockk()
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
    val bekreftelseMelding = bekreftelseMelding(periodeId = periode1.id)
    val opplysningerId = UUID.randomUUID()
    every { databaseQuerySupportMock.hentPerioder(Identitetsnummer(periode1.identitetsnummer)) } returns listOf(periode1.id)
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
            data = createProfilering(periodeId = periode1.id, opplysningerId = opplysningerId).toOpenApi(),
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
    "Verifiser at endepunkter fungerer" - {
        "/api/v1/veileder/arbeidssoekerperioder" {
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
                        ident = ansatt1.verdi,
                        sikkerhetsnivaa = "tokenx:Level4"
                    )
                )
                val response = client.hentViaPost(
                    url = "${V1_API_BASE_PATH}/${V1_API_VEILEDER_ARBEIDSSOEKERPERIODER}",
                    token = token,
                    request = ArbeidssoekerperiodeRequest(periode1.identitetsnummer)
                )
                response.status shouldBe HttpStatusCode.OK
                val body: List<ArbeidssoekerperiodeResponse> = response.body()
                body.size shouldBe 1
                val periode = body.first()

                periode.periodeId shouldBe periode1.id
                periode.startet.tidspunkt shouldBe periode1.startet.tidspunkt
                periode.startet.utfoertAv.id shouldBe periode1.startet.utfoertAv.id
                periode.startet.utfoertAv.type.name shouldBe periode1.startet.utfoertAv.type.name
                periode.avsluttet?.tidspunkt shouldBe periode1.avsluttet?.tidspunkt
                periode.avsluttet?.utfoertAv?.id shouldBe periode1.avsluttet?.utfoertAv?.id
                periode.avsluttet?.utfoertAv?.type?.name shouldBe periode1.avsluttet?.utfoertAv?.type?.name
            }
        }
        "/api/v1/${V1_API_VEILEDER_PROFILERING}" {
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
                        ident = ansatt1.verdi,
                        sikkerhetsnivaa = "tokenx:Level4"
                    )
                )
                val response = client.hentViaPost(
                    url = "${V1_API_BASE_PATH}/${V1_API_VEILEDER_PROFILERING}",
                    token = token,
                    request = ProfileringRequest(periode1.identitetsnummer, periode1.id)
                )
                response.status shouldBe HttpStatusCode.OK
                val body: List<ProfileringResponse> = response.body()
                body.size shouldBe 3
                val profilering = body.first()
                profilering.periodeId shouldBe periode1.id
            }
        }
        "/api/v1/${V1_API_VEILEDER_ARBEIDSSOEKERPERIODER_AGGREGERT}" {
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
                        ident = ansatt1.verdi,
                        sikkerhetsnivaa = "tokenx:Level4"
                    )
                )
                val response = client.hentViaPost(
                    url = "${V1_API_BASE_PATH}/${V1_API_VEILEDER_ARBEIDSSOEKERPERIODER_AGGREGERT}",
                    token = token,
                    request = ArbeidssoekerperiodeRequest(periode1.identitetsnummer)
                )
                response.status shouldBe HttpStatusCode.OK
                val body: List<ArbeidssoekerperiodeAggregertResponse> = response.body()
                body.size shouldBe 1
                val data = body.first()
                data.opplysningerOmArbeidssoeker?.size shouldBe 1
                data.opplysningerOmArbeidssoeker?.firstOrNull()?.profilering shouldNotBe null
                data.opplysningerOmArbeidssoeker?.firstOrNull()?.opplysningerOmArbeidssoekerId shouldBe
                        data.opplysningerOmArbeidssoeker?.firstOrNull()?.profilering?.opplysningerOmArbeidssoekerId
            }
        }
    }
})


