package no.naw.paw.brukerprofiler

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.brukerprofiler.api.Brukerprofil

class BrukerprofilRouteTest : FreeSpec({
    val oauthServer = MockOAuth2Server()
    beforeSpec { oauthServer.start() }
    afterSpec { oauthServer.shutdown() }
    val brukerprofilTjeneste = mockk<BrukerprofilTjeneste>(relaxed = true).also {
        coEvery { it.kanTilbysTjenesten(any()) } returns true
    }

    "Happy path" {
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            routing {
                brukerprofilRoute(brukerprofilTjeneste)
            }
            val testIdent = Identitetsnummer("12345678901")
            val token: String = oauthServer.personToken(id = testIdent)!!
            val response = testClient().get(BRUKERPROFIL_PATH) {
                bearerAuth(token)
                contentType(Application.Json)
            }
            response.validateAgainstOpenApiSpec()
            response.status shouldBe HttpStatusCode.OK
            response.body<Brukerprofil>().identitetsnummer shouldBe testIdent.verdi
        }
    }

})

private val MockOAuth2Server.tokenXAuthProvider: AuthProvider
    get() = AuthProvider(
        name = TokenX.name,
        audiences = listOf("default"),
        discoveryUrl = wellKnownUrl("default").toString(),
        requiredClaims = AuthProviderRequiredClaims(
            listOf("acr=Level4", "acr=idporten-loa-high"),
            true
        )
    )

private fun MockOAuth2Server.personToken(
    id: Identitetsnummer,
    acr: String = "idporten-loa-high",
): String? =
    mapOf(
        "acr" to acr,
        "pid" to id.verdi
    ).let {
        it.plus("issuer" to "tokenx") to issueToken(claims = it)
    }.second.serialize()

private fun ApplicationTestBuilder.testClient(): HttpClient = createClient {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
