package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumers

import io.kotest.core.spec.style.FreeSpec
import io.mockk.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.producers.TestMessages
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.concurrent.thread

class BatchConsumerTest : FreeSpec({

    "should consume and process messages when startet and stop when stopped" {
        val topic = "test-topic"
        val consumerMock = mockk<KafkaConsumer<Long, Periode>>()
        val serviceMock = mockk<ArbeidssoekerperiodeService>()

        val consumer = BatchConsumer(topic, consumerMock, serviceMock::lagreBatch)

        every { consumerMock.subscribe(listOf(topic)) } just Runs
        every { consumerMock.unsubscribe() } just Runs
        every { consumerMock.close() } just Runs
        every { consumerMock.poll(any<Duration>()) } returns createConsumerRecords()
        every { serviceMock.lagreBatch(any()) } just Runs
        every { consumerMock.commitSync() } just Runs

        thread {
            consumer.subscribe()
            consumer.getAndProcessBatch(topic)
        }

        verify { consumerMock.subscribe(listOf(topic)) }
        verify { consumerMock.poll(any<Duration>()) }
        verify { serviceMock.lagreBatch(any()) }
        verify { consumerMock.commitSync() }

        consumer.stop()

        verify { consumerMock.unsubscribe() }
        verify { consumerMock.close() }
    }
})

private fun createConsumerRecords(): ConsumerRecords<Long, Periode> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Periode>>>()
    val topic = "test-topic"
    records[TopicPartition(topic, 0)] = mutableListOf(ConsumerRecord(topic, 0, 0, 1L, TestMessages().perioder()[0]))
    return ConsumerRecords(records)
}
