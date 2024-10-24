package no.nav.paw.arbeidssoekerregisteret.eksternt.api.consumers

import io.kotest.core.spec.style.FreeSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.PeriodeConsumer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ArbeidssoekerService
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread

class PeriodeConsumerTest : FreeSpec({
    "should consume and process messages when started and stop when stopped" {
        val topic = "test-topic"
        val consumerMock = mockk<KafkaConsumer<Long, Periode>>()
        val serviceMock = mockk<ArbeidssoekerService>()

        val consumer = PeriodeConsumer(topic, consumerMock, serviceMock)

        every { consumerMock.subscribe(listOf(topic)) } just Runs
        every { consumerMock.unsubscribe() } just Runs
        every { consumerMock.poll(any<Duration>()) } returns createConsumerRecords()
        every { serviceMock.storeBatch(any()) } just Runs
        every { consumerMock.commitSync() } just Runs

        thread {
            consumer.start()
        }

        verify { consumerMock.subscribe(listOf(topic)) }
        verify { consumerMock.poll(any<Duration>()) }
        verify { serviceMock.storeBatch(any()) }
        verify { consumerMock.commitSync() }

        consumer.stop()

        verify { consumerMock.unsubscribe() }
    }
})

private fun createConsumerRecords(): ConsumerRecords<Long, Periode> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Periode>>>()
    val topic = "test-topic"
    records[TopicPartition(topic, 0)] =
        mutableListOf(
            ConsumerRecord(
                topic, 0, 0, 1L,
                Periode(
                    UUID.randomUUID(),
                    "12345678901",
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            "12345678901"
                        ),
                        "test",
                        "test"
                    ),
                    null
                )
            )
        )
    return ConsumerRecords(records)
}
