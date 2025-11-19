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
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
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
import no.nav.paw.oppslagapi.routes.v1.v1Routes
import no.nav.paw.oppslagapi.routes.v2.v2Routes
import no.nav.paw.oppslagapi.routes.v3.v3Routes
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.slf4j.event.Level
import java.time.Duration

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
        level = Level.TRACE
        filter { call ->
            !call.request.path().contains("internal")
        }
    }
    install(ErrorHandlingPlugin)
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        this.meterBinders = meterBinders
        distributionStatisticConfig =
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofSeconds(1).toNanos().toDouble())
                .minimumExpectedValue(Duration.ofMillis(20).toNanos().toDouble())
                .serviceLevelObjectives(
                    Duration.ofMillis(150).toNanos().toDouble(),
                    Duration.ofMillis(500).toNanos().toDouble()
                )
                .build()
    }
    installAuthenticationPlugin(authProviders)
}

fun <A> Route.configureRoutes(
    healthIndicator: A,
    prometheusRegistry: PrometheusMeterRegistry,
    appQueryLogic: ApplicationQueryLogic
) where A : IsAlive, A : IsReady, A : HasStarted {
    internalRoutes(healthIndicator, prometheusRegistry)
    swaggerUI(path = "/api/docs/v1", swaggerFile = "openapi/v1-spec.yaml")
    swaggerUI(path = "/api/docs/v2", swaggerFile = "openapi/v2-spec.yaml")
    swaggerUI(path = "/api/docs/v3", swaggerFile = "openapi/v3-spec.yaml")
    v1Routes(appQueryLogic)
    v2Routes(appQueryLogic)
    v3Routes(appQueryLogic)
}

suspend inline fun <reified T : Any> RoutingCall.respondWith(response: Response<T>) {
    when (response) {
        is Data<T> -> {
            respond(status = HttpStatusCode.OK, message = response.data)
        }

        is ProblemDetails -> {
            respond(status = response.status, message = response)
        }
    }
}

