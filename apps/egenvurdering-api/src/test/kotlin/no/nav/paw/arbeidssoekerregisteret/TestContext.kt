package no.nav.paw.arbeidssoekerregisteret

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.routes.egenvurderingRoutes
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.utils.configureJackson
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Serializer

open class TestContext {

    val SCHEMA_REGISTRY_SCOPE = "test-registry"

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val mockOAuth2Server = MockOAuth2Server()
    val meterRegistry = SimpleMeterRegistry()
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val prometheusMeterRegistryMock = mockk<PrometheusMeterRegistry>()
    val authorizationService = AuthorizationService()
    val egenvurderingService = mockk<EgenvurderingService>().also {
        coEvery { it.getEgenvurderingGrunnlag(any()) } returns EgenvurderingGrunnlag(grunnlag = null)
    }

    fun ApplicationTestBuilder.configureTestApplication() {
        val applicationContext = ApplicationContext(
            serverConfig,
            applicationConfig,
            securityConfig.copy(authProviders = securityConfig.authProviders.map {
                it.copy(
                    audiences = listOf("default"),
                    discoveryUrl = mockOAuth2Server.wellKnownUrl("default").toString()
                )
            }),
            prometheusMeterRegistryMock,
            authorizationService,
            kafkaKeysClientMock,
            egenvurderingService,
            mockk<Serializer<Egenvurdering>>(relaxed = true),
            mockk<Producer<Long, Egenvurdering>>(relaxed = true),
            datasource = mockk(relaxed = true),
        )
        application {
            configureSerialization()
            configureHTTP()
            configureAuthentication(applicationContext)
        }
        routing {
            egenvurderingRoutes(authorizationService, egenvurderingService)
        }
    }

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }
}