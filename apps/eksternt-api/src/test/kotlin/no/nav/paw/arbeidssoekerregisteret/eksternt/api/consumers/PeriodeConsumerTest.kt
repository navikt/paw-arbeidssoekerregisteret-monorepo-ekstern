package no.nav.paw.arbeidssoekerregisteret.eksternt.api.consumers

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.TestData
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.concurrent.thread

class PeriodeConsumerTest : FreeSpec({
    with(ApplicationTestContext.withRealDataAccess()) {

        "Should consume and process messages" {
            every { periodeKafkaConsumerMock.subscribe(any<List<String>>()) } just Runs
            every { periodeKafkaConsumerMock.unsubscribe() } just Runs
            every { periodeKafkaConsumerMock.poll(any<Duration>()) } returns createConsumerRecords(kafkaTopic)
            every { periodeKafkaConsumerMock.commitSync() } just Runs

            thread {
                periodeConsumer.start()
            }

            delay(100)

            periodeConsumer.stop()

            delay(100)

            val perioder = periodeRepository.finnPerioder(identitetsnummer = TestData.identitetsnummer1)
            periodeRepository.hentAntallAktivePerioder() shouldBe 3

            perioder.size shouldBe 3
            verify { periodeKafkaConsumerMock.subscribe(any<List<String>>()) }
            verify { periodeKafkaConsumerMock.unsubscribe() }
            verify { periodeKafkaConsumerMock.poll(any<Duration>()) }
            verify { periodeKafkaConsumerMock.commitSync() }
        }
    }
})

private fun createConsumerRecords(topic: String): ConsumerRecords<Long, Periode> {
    val records = mutableMapOf<TopicPartition, MutableList<ConsumerRecord<Long, Periode>>>()
    records[TopicPartition(topic, 0)] = mutableListOf(
        TestData.nyConsumerRecord(
            topic = topic,
            offset = 0,
            value = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
        ),
        TestData.nyConsumerRecord(
            topic = topic,
            offset = 1,
            value = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
        ),
        TestData.nyConsumerRecord(
            topic = topic,
            offset = 2,
            value = TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
        )
    )
    return ConsumerRecords(records)
}
