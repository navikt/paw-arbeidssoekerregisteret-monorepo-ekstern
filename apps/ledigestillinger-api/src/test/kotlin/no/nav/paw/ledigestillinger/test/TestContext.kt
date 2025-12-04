package no.nav.paw.ledigestillinger.test

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.test.TestResult
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.healthChecksOf
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.context.TelemetryContext
import no.nav.paw.ledigestillinger.plugin.configureRouting
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import java.time.Duration
import javax.sql.DataSource
import kotlin.coroutines.EmptyCoroutineContext

class TestContext private constructor(
    val clock: MutableClock = TestData.clock,
    val dataSource: DataSource,
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
        .copy(beholdAlleStillingerPublisertEtter = clock.instant().minus(Duration.ofDays(100))),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    val stillingService: StillingService = StillingService(
        clock = clock,
        applicationConfig = applicationConfig,
        telemetryContext = TelemetryContext(meterRegistry)
    )
) {
    fun setUp(): TestContext {
        Database.connect(dataSource)
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .cleanDisabled(false)
            .load()
        flyway.clean()
        flyway.migrate()
        return this
    }

    fun tearDown(): TestContext {
        dataSource.connection.close()
        return this
    }

    private fun MockOAuth2Server.authProviders(): List<AuthProvider> {
        val wellKnownUrl = wellKnownUrl("default").toString()
        return securityConfig.authProviders.map {
            it.copy(
                discoveryUrl = wellKnownUrl,
                audiences = listOf("default")
            )
        }
    }

    fun securedTestApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult {
        return testApplication(EmptyCoroutineContext) {
            application {
                installContentNegotiationPlugin()
                installErrorHandlingPlugin()
                installAuthenticationPlugin(mockOAuth2Server.authProviders())
                configureRouting(
                    healthChecks = healthChecksOf(),
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    stillingService = stillingService
                )
            }
            block()
        }
    }

    fun ApplicationTestBuilder.buildTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }

    fun MockOAuth2Server.issueTokenXToken(
        acr: String = "idporten-loa-high",
        pid: String = TestData.fnr1
    ): SignedJWT {
        return issueToken(
            claims = mapOf(
                "acr" to acr,
                "pid" to pid
            )
        )
    }

    inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    companion object {
        fun buildWithDatabase(): TestContext {
            return TestContext(dataSource = buildPostgresDataSource())
        }
    }
}