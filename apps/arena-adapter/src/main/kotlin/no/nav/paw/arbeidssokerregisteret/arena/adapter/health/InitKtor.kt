package no.nav.paw.arbeidssokerregisteret.arena.adapter.health

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.liveness.livenessRoute
import no.nav.paw.health.probes.isAlive
import no.nav.paw.health.probes.isReady
import no.nav.paw.health.readiness.readinessRoute
import org.apache.kafka.streams.KafkaStreams

fun initKtor(
    kafkaStreamsMetrics: KafkaStreamsMetrics,
    prometheusRegistry: PrometheusMeterRegistry,
    kafkaStreams: KafkaStreams,
) = embeddedServer(Netty, port = 8080) {
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
        livenessRoute({ kafkaStreams.isAlive() })
        readinessRoute({ kafkaStreams.isReady() })
        get("/internal/metrics") {
            call.respond(prometheusRegistry.scrape())
        }
    }
}
