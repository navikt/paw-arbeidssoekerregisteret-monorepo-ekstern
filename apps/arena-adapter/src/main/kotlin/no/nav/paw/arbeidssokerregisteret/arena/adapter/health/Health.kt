package no.nav.paw.arbeidssokerregisteret.arena.adapter.health

import org.apache.kafka.streams.KafkaStreams

class Health(private val kafkaStreams: KafkaStreams) {
    fun alive(): Boolean {
        val state = kafkaStreams.state()
        return when (state) {
            KafkaStreams.State.CREATED -> true
            KafkaStreams.State.REBALANCING -> true
            KafkaStreams.State.RUNNING -> true
            else -> false
        }
    }

    fun ready(): Boolean {
        val state = kafkaStreams.state()
        return when (state) {
            KafkaStreams.State.CREATED -> true
            KafkaStreams.State.REBALANCING -> true
            KafkaStreams.State.RUNNING -> true
            else -> false
        }
    }
}
