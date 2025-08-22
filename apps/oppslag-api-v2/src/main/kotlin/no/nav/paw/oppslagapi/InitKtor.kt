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
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.plugin.ErrorHandlingPlugin
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.health.HasStarted
import no.nav.paw.oppslagapi.health.IsAlive
import no.nav.paw.oppslagapi.health.IsReady
import no.nav.paw.oppslagapi.health.internalRoutes
import no.nav.paw.oppslagapi.routes.v2Bekreftelse
import no.nav.paw.oppslagapi.routes.v2Tidslinjer
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.slf4j.event.Level

fun <A> initEmbeddedKtorServer(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    healthIndicator: A,
    authProviders: List<AuthProvider>,
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
    appQueryLogic: ApplicationQueryLogic
) where A : IsAlive, A : IsReady, A : HasStarted {
    internalRoutes(healthIndicator, prometheusRegistry)
    swaggerUI(path = "documentation/openapi-spec", swaggerFile = "openapi/openapi-spec.yaml")
    swaggerUI(path = "documentation/legacy-spec", swaggerFile = "openapi/legacy-spec.yaml")
    route("/api/v2/bekreftelser") {
        v2Bekreftelse(appQueryLogic)
    }
    route("/api/v2/tidslinjer") {
        v2Tidslinjer(appQueryLogic)
    }
}

suspend inline fun <reified T: Any> RoutingCall.respondWith(response: Response<T>) {
    when (response) {
        is Data<T> -> {
            respond(status = HttpStatusCode.OK, message = response.data)
        }
        is ProblemDetails -> {
            respond(status = response.status, message = response)
        }
    }
}

