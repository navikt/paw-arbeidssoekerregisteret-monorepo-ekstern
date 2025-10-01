package no.nav.paw.health.probes

import no.nav.paw.health.liveness.LivenessCheck
import no.nav.paw.health.readiness.ReadinessCheck
import org.apache.kafka.streams.KafkaStreams

class KafkaStreamsHealthProbe(private val kafkaStreams: KafkaStreams): LivenessCheck, ReadinessCheck {
    override fun isAlive() = when (kafkaStreams.state()) {
        KafkaStreams.State.CREATED -> true
        KafkaStreams.State.REBALANCING -> true
        KafkaStreams.State.RUNNING -> true
        else -> false
    }

    override fun isReady() = when (kafkaStreams.state()) {
        KafkaStreams.State.RUNNING -> true
        KafkaStreams.State.REBALANCING -> true
        else -> false
    }
}
