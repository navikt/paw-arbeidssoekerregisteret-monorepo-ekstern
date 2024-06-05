package no.nav.paw.arbeidssoekerregisteret.routes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.common.audit_log.log.AuditLoggerConstants.AUDIT_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.AuthProvider
import no.nav.paw.arbeidssoekerregisteret.config.AuthProviders
import no.nav.paw.arbeidssoekerregisteret.config.RequiredClaims
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleRequest
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.arbeidssoekerregisteret.topology.TEST_APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.slf4j.LoggerFactory

@Ignored // TODO Få authz til å funke i test
class ToggleRoutesTest : FreeSpec({
    with(ToggleRoutesTestContext()) {
        afterSpec {
            mockOAuth2Server.shutdown()
        }

        testApplication {
            with(ConfigContext(appConfig)) {
                with(LoggingContext(logger, auditLogger)) {
                    application {
                        configureSerialization()
                        configureRequestHandling()
                        configureAuthentication(mockOAuth2Server.buildAuthProviders())
                    }
                    routing {
                        toggleRoutes(toggleService)
                    }
                }
            }
            val restClient = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }

            "Test av toggle routes" - {

                "Skal få 401 uten Bearer token" {
                    val response = restClient.post("/api/v1/microfrontend-toggle") {
                        contentType(ContentType.Application.Json)
                        setBody(ToggleRequest(ToggleAction.ENABLE, "aia-min-side"))
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }
})

fun MockOAuth2Server.buildAuthProviders(): AuthProviders {
    return listOf(
        AuthProvider(
            name = "tokenx",
            clientId = "paw-microfrontend-toggler",
            discoveryUrl = wellKnownUrl("default").toString(),
            tokenEndpointUrl = tokenEndpointUrl("default").toString(),
            requiredClaims = RequiredClaims(listOf(), true)
        )
    )
}

class ToggleRoutesTestContext {
    val logger = LoggerFactory.getLogger(APPLICATION_LOGGER_NAME)
    val auditLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val kafkaProducerMock = mockk<Producer<Long, Toggle>>()
    val toggleService = ToggleService(kafkaKeysClientMock, kafkaProducerMock)
    val mockOAuth2Server = buildAndStartMockOAuth2Server()

    private fun buildAndStartMockOAuth2Server(): MockOAuth2Server {
        val mockOAuth2Server = MockOAuth2Server()
        mockOAuth2Server.start()
        return mockOAuth2Server
    }
}