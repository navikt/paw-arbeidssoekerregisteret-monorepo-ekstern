package no.nav.paw.arbeidssoekerregisteret.config

import org.apache.kafka.streams.KafkaStreams

enum class HealthStatus(val text: String) {
    PENDING("PENDING"),
    HEALTHY("HEALTHY"),
    UNHEALTHY("UNHEALTHY"),
}

enum class RunnableHealthStatus {
    STARTING,
    RUNNING,
    FAILED
}

class KafkaStreamsHealthIndicator : KafkaStreams.StateListener {
    override fun onChange(newState: KafkaStreams.State, oldState: KafkaStreams.State) {
        TODO("Not yet implemented")
    }
}
