package no.nav.paw.health.probes

import org.apache.kafka.streams.KafkaStreams

fun KafkaStreams.isAlive(): Boolean {
    return when (state()) {
        KafkaStreams.State.CREATED -> true
        KafkaStreams.State.REBALANCING -> true
        KafkaStreams.State.RUNNING -> true
        else -> false
    }
}

fun KafkaStreams.isReady(): Boolean {
    return when (state()) {
        KafkaStreams.State.RUNNING -> true
        KafkaStreams.State.REBALANCING -> true
        else -> false
    }
}