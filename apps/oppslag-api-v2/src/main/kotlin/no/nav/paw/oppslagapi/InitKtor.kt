package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.plugin.ErrorHandlingPlugin
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.health.HasStarted
import no.nav.paw.oppslagapi.health.IsAlive
import no.nav.paw.oppslagapi.health.IsReady
import no.nav.paw.oppslagapi.health.internalRoutes
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.slf4j.event.Level

fun <A> initEmbeddedKtorServer(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    healthIndicator: A,
    authProviders: List<AuthProvider>,
    openApiSpecFile: String = "openapi/openapi-spec.yaml",
    appQueryLogic: ApplicationQueryLogic,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> where
        A : IsAlive, A : IsReady, A : HasStarted {

    return embeddedServer(Netty, port = 8080) {
        configureKtorServer(
            prometheusRegistry = prometheusRegistry,
            meterBinders = meterBinders,
            authProviders = authProviders
        )
        routing {
            configureRoutes(
                healthIndicator = healthIndicator,
                prometheusRegistry = prometheusRegistry,
                openApiSpecFile = openApiSpecFile,
                appQueryLogic = appQueryLogic
            )
        }
    }
}

fun Application.configureKtorServer(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    authProviders: List<AuthProvider>
) {
    installContentNegotiationPlugin()
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            !call.request.path().contains("internal")
        }
    }
    install(ErrorHandlingPlugin)
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        this.meterBinders = meterBinders
    }
    installAuthenticationPlugin(authProviders)
}

fun <A> Route.configureRoutes(
    healthIndicator: A,
    prometheusRegistry: PrometheusMeterRegistry,
    openApiSpecFile: String,
    appQueryLogic: ApplicationQueryLogic
) where A : IsAlive, A : IsReady, A : HasStarted {
    internalRoutes(healthIndicator, prometheusRegistry)
    swaggerUI(path = "documentation/openapi-spec", swaggerFile = openApiSpecFile)
    route("/api/v2/bekreftelser") {
        autentisering(TokenX, AzureAd) {
            post<ApiV2BekreftelserPostRequest> { request ->
                val securityContext = call.securityContext()
                val response = appQueryLogic.hentBekreftelser(
                    bruker = securityContext.bruker,
                    request = request
                )
                when (response) {
                    is Data<BekreftelserResponse> -> {
                        call.respond(HttpStatusCode.OK, response.data)
                    }
                    is ProblemDetails -> {
                        call.respond(response.status, response)
                    }
                }
            }
        }
    }
}
