package no.nav.paw.ledigestillinger

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.context.ApplicationContext
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.slf4j.event.Level

fun <A> initEmbeddedKtorServer(
    applicationContext: ApplicationContext
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> where
        A : LivenessCheck, A : ReadinessCheck, A : StartupCheck {
    with(applicationContext) {
        return embeddedServer(Netty, port = 8080) {
            configureKtorServer(
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                meterRegistry = meterRegistry,
                meterBinders = meterBinders,
                //dataSource = dataSource
            )
            routing {
                livenessRoute(healthChecks)
                readinessRoute(healthChecks)
                startupRoute(healthChecks)
                metricsRoutes(meterRegistry)
            }
        }
    }
}

fun Application.configureKtorServer(
    applicationConfig: ApplicationConfig,
    securityConfig: SecurityConfig,
    meterRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    //dataSource: DataSource
) {
    installContentNegotiationPlugin()
    install(CallLogging) {
        level = Level.TRACE
        filter { call ->
            !call.request.path().contains("internal")
        }
    }
    installErrorHandlingPlugin()
    installWebAppMetricsPlugin(
        meterRegistry = meterRegistry,
        additionalMeterBinders = meterBinders
    )
    installAuthenticationPlugin(securityConfig.authProviders)
    //installDatabasePlugin(dataSource)
}

