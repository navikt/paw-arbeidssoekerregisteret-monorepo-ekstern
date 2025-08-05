package no.nav.paw.health.probes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.streams.KafkaStreams

class KafkaStreamsHealthProbeTest : FreeSpec({

    val alleKafkaStreamsStates = KafkaStreams.State.entries

    "Kafkastream.isAlive() returnerer true for CREATED, REBALANCING, RUNNING" {
        val kafkaStreams = mockk<KafkaStreams>()
        val aliveStates = listOf(
            KafkaStreams.State.CREATED,
            KafkaStreams.State.REBALANCING,
            KafkaStreams.State.RUNNING
        )

        alleKafkaStreamsStates.forEach { state ->
            every { kafkaStreams.state() } returns state

            if (state in aliveStates) {
                kafkaStreams.isAlive() shouldBe true
            } else {
                kafkaStreams.isAlive() shouldBe false
            }
        }
    }

    "Kafkastream.isReady() returnerer true for REBALANCING, RUNNING" {
        val kafkaStreams = mockk<KafkaStreams>()
        val readyStates = listOf(
            KafkaStreams.State.REBALANCING,
            KafkaStreams.State.RUNNING
        )

        alleKafkaStreamsStates.forEach { state ->
            every { kafkaStreams.state() } returns state

            if (state in readyStates) {
                kafkaStreams.isReady() shouldBe true
            } else {
                kafkaStreams.isReady() shouldBe false
            }
        }
    }
})
