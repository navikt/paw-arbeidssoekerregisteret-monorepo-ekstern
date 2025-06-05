package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
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
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun initKtor(
    prometheusRegistry: PrometheusMeterRegistry,
    dataConsumerTask: DataConsumer,
    meterBinders: List<MeterBinder>
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusRegistry
            this.meterBinders = meterBinders
        }
        routing {
            route("internal") {
                route("isAlive") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.Companion.OK)
                    }
                }
                route("isReady") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.Companion.OK)
                    }
                }
                route("isStarted") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.Companion.OK)
                    }
                }
                route("metrics") {
                    get {
                        call.respondText(prometheusRegistry.scrape())
                    }
                }
                route("lastPoll") {
                    get {
                        call.respondText(
                            dataConsumerTask.sisteProessering.toString(),
                            status = HttpStatusCode.Companion.OK
                        )
                    }
                }
            }
        }
    }