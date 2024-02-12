package no.nav.paw.arbeidssokerregisteret.arena.adapter.health

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry

fun initKtor(
    kafkaStreamsMetrics: KafkaStreamsMetrics,
    prometheusRegistry: PrometheusMeterRegistry,
    health: Health
): ApplicationEngine {
    return embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusRegistry
            meterBinders = listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                kafkaStreamsMetrics
            )
        }
        routing {
            get("/isReady") {
                val status = health.ready()
                call.respond(status.code, status.message)
            }
            get("/isAlive") {
                val alive = health.alive()
                call.respond(alive.code, alive.message)
            }
            get("/metrics") {
                call.respond(prometheusRegistry.scrape())
            }
        }
    }
}
