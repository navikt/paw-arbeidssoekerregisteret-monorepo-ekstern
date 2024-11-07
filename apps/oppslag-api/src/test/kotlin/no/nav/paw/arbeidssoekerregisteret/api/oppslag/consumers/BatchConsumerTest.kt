package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumers

import io.kotest.core.spec.style.FreeSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.BatchKafkaConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
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
        val serviceMock = mockk<PeriodeService>()

        val consumer = BatchKafkaConsumer(topic, consumerMock, serviceMock::lagreAllePerioder)

        every { consumerMock.subscribe(any<List<String>>()) } just Runs
        every { consumerMock.unsubscribe() } just Runs
        every { consumerMock.close() } just Runs
        every { consumerMock.poll(any<Duration>()) } returns createConsumerRecords()
        every { serviceMock.lagreAllePerioder(any<Sequence<Periode>>()) } just Runs
        every { consumerMock.commitSync() } just Runs

        thread {
            consumer.subscribe()
            consumer.getAndProcessBatch(topic)
        }

        delay(100)

        verify { consumerMock.subscribe(any<List<String>>()) }
        verify { consumerMock.poll(any<Duration>()) }
        verify { serviceMock.lagreAllePerioder(any()) }
        verify { consumerMock.commitSync() }

        consumer.stop()

        verify { consumerMock.unsubscribe() }
        verify { consumerMock.close() }
    }
})

private fun createConsumerRecords(): ConsumerRecords<Long, Periode> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Periode>>>()
    val topic = "test-topic"
    records[TopicPartition(topic, 0)] = mutableListOf(ConsumerRecord(topic, 0, 0, 1L, TestData.nyStartetPeriode()))
    return ConsumerRecords(records)
}
