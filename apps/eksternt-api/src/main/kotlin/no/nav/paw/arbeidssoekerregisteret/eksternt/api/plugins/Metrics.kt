package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.configureMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    consumer: KafkaConsumer<*, *>
) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders =
            listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                KafkaClientMetrics(consumer)
            )
    }
}
