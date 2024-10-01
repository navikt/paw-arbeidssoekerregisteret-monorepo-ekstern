package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun Application.configureMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    vararg consumer: KafkaConsumer<*, *>
) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders =
            listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics()
            ) + consumer.map { KafkaClientMetrics(it) }
        distributionStatisticConfig =
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofSeconds(1).toNanos().toDouble())
                .minimumExpectedValue(Duration.ofMillis(10).toNanos().toDouble())
                .serviceLevelObjectives(
                    Duration.ofMillis(50).toNanos().toDouble(),
                    Duration.ofMillis(100).toNanos().toDouble()
                )
                .build()
    }
}
