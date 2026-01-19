package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test

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
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import kotlinx.coroutines.test.TestResult
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdRepository
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.configureJacksonOverrides
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.healthChecksOf
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.*
import javax.sql.DataSource
import kotlin.coroutines.EmptyCoroutineContext

class TestContext private constructor(
    val dataSource: DataSource? = null,
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val veilarbdialogClientMock: VeilarbdialogClient = mockk<VeilarbdialogClient>(),
    val periodeIdDialogIdRepositoryMock: PeriodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>(),
    val dialogService: DialogService = DialogService(
        veilarbdialogClient = veilarbdialogClientMock,
        periodeIdDialogIdRepository = periodeIdDialogIdRepositoryMock
    ),
    val dialogServiceMock: DialogService = mockk<DialogService>()
) {
    fun setUpDatabase(): TestContext {
        Database.connect(dataSource!!)
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .cleanDisabled(false)
            .load()
        flyway.clean()
        flyway.migrate()
        return this
    }

    fun tearDownDatabase(): TestContext {
        dataSource!!.connection.close()
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

    fun securedTestApplication(service: DialogService = dialogService, block: suspend ApplicationTestBuilder.() -> Unit): TestResult {
        return testApplication(EmptyCoroutineContext) {
            application {
                installContentNegotiationPlugin {
                    configureJacksonOverrides()
                }
                installErrorHandlingPlugin()
                installAuthenticationPlugin(mockOAuth2Server.authProviders())
                configureRouting(
                    healthChecks = healthChecksOf(),
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    dialogService = service,
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
        pid: String = "01017012345",
    ): SignedJWT {
        return issueToken(
            claims = mapOf(
                "acr" to acr,
                "pid" to pid
            )
        )
    }

    fun MockOAuth2Server.issueAzureAdToken(
        oid: UUID = UUID.randomUUID(),
        name: String = "Kari Nordmann",
        navIdent: String = "NAV1234",
    ): SignedJWT {
        return issueToken(
            claims = mapOf(
                "oid" to oid.toString(),
                "name" to name,
                "NAVident" to navIdent
            )
        )
    }

    inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    companion object {
        fun build(): TestContext = TestContext()
        fun buildWithDatabase(): TestContext = TestContext(dataSource = buildPostgresDataSource())
    }
}