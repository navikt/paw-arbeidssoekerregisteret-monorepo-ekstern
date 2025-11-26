package no.nav.paw.arbeidssoekerregisteret.test

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.installTracingPlugin
import no.nav.paw.arbeidssoekerregisteret.plugins.installWebPlugins
import no.nav.paw.arbeidssoekerregisteret.route.egenvurderingRoutes
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.utils.configureJacksonOverrides
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.GenericLivenessProbe
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Serializer

open class TestContext {

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val mockOAuth2Server = MockOAuth2Server()
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val prometheusMeterRegistryMock = mockk<PrometheusMeterRegistry>()
    val egenvurderingService = mockk<EgenvurderingService>()

    fun ApplicationTestBuilder.configureTestApplication() {
        val applicationContext = ApplicationContext(
            serverConfig = serverConfig,
            applicationConfig = applicationConfig,
            securityConfig = securityConfig.copy(authProviders = securityConfig.authProviders.map {
                it.copy(
                    audiences = listOf("default"),
                    discoveryUrl = mockOAuth2Server.wellKnownUrl("default").toString()
                )
            }),
            prometheusMeterRegistry = prometheusMeterRegistryMock,
            kafkaKeysClient = kafkaKeysClientMock,
            egenvurderingService = egenvurderingService,
            egenvurderingAvroSerializer = mockk<Serializer<Egenvurdering>>(relaxed = true),
            producer = mockk<Producer<Long, Egenvurdering>>(relaxed = true),
            datasource = mockk(relaxed = true),
            consumer = mockk(relaxed = true),
            topics = standardTopicNames(currentRuntimeEnvironment),
            hwmRebalanceListener = mockk(relaxed = true),
            kafkaConsumerLivenessProbe = GenericLivenessProbe(),
            healthChecks = healthChecksOf()
        )
        application {
            installWebPlugins()
            installContentNegotiationPlugin {
                configureJacksonOverrides()
            }
            installTracingPlugin()
            installErrorHandlingPlugin()
            installAuthenticationPlugin(applicationContext.securityConfig.authProviders)
        }
        routing {
            egenvurderingRoutes(egenvurderingService)
        }
    }

    inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJacksonOverrides()
                }
            }
        }
    }

    fun MockOAuth2Server.issueTokenXToken(
        issuerId: String = "default",
        acr: String = "idporten-loa-high",
        pid: String = "12345678901"
    ): SignedJWT {
        return issueToken(
            issuerId = issuerId,
            claims = mapOf(
                "acr" to acr,
                "pid" to pid
            )
        )
    }
}