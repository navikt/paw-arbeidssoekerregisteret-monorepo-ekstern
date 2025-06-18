package no.nav.paw.oppslagapi

import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.oppslagapi.health.HasStarted
import no.nav.paw.oppslagapi.health.HealthIndicator
import no.nav.paw.oppslagapi.health.IsAlive
import no.nav.paw.oppslagapi.health.IsReady
import no.nav.paw.oppslagapi.health.httpResponse

fun <A> initKtor(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    healthIndicator: A
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
        where A : IsAlive, A : IsReady, A : HasStarted =
    embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusRegistry
            this.meterBinders = meterBinders
        }
        routing {
            route("internal") {
                route("isAlive") {
                    get {
                        val (code, message) = healthIndicator.isAlive().httpResponse()
                        call.respondText(text = message, status = code)
                    }
                }
                route("isReady") {
                    get {
                        val (code, message) = healthIndicator.isReady().httpResponse()
                        call.respondText(text = message, status = code)
                    }
                }
                route("hasStarted") {
                    get {
                        val (code, message) = healthIndicator.hasStarted().httpResponse()
                        call.respondText(text = message, status = code)
                    }
                }
                route("metrics") {
                    get {
                        call.respondText(prometheusRegistry.scrape())
                    }
                }
            }
        }
    }