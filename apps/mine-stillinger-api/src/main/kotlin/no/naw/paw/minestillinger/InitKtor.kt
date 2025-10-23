package no.naw.paw.minestillinger

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
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.error.plugin.ErrorHandlingPlugin
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.route.brukerprofilRoute
import no.naw.paw.minestillinger.route.kodeverk
import org.slf4j.event.Level
import java.time.Duration

fun <A> initEmbeddedKtorServer(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    healthIndicator: A,
    authProviders: List<AuthProvider>,
    brukerprofilTjeneste: BrukerprofilTjeneste,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> where
        A : LivenessCheck, A : ReadinessCheck, A : StartupCheck {

    return embeddedServer(Netty, port = 8080) {
        configureKtorServer(
            prometheusRegistry = prometheusRegistry,
            meterBinders = meterBinders,
            authProviders = authProviders
        )
        routing {
            livenessRoute(healthIndicator)
            readinessRoute(healthIndicator)
            startupRoute(healthIndicator)
            brukerprofilRoute(brukerprofilTjeneste)
            kodeverk()
            swaggerUI("/docs/api", "openapi/openapi-spec.yaml")
            get("/internal/metrics") {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = prometheusRegistry.scrape()
                )
            }
        }
    }
}

fun Application.configureKtorServer(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    authProviders: List<AuthProvider>,
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

