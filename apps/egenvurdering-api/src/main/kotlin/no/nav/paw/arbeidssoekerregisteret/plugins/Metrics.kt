package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import java.time.Duration

fun Application.configureMetrics(applicationContext: ApplicationContext) {
    with(applicationContext) {
        val metricsMeterBinders = mutableListOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        )
        val kafkaClientMetrics = listOf(KafkaClientMetrics(producer))
        metricsMeterBinders.addAll(kafkaClientMetrics)
        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
            meterBinders = metricsMeterBinders
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
    }
}
