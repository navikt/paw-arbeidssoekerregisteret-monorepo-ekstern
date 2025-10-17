package no.nav.paw.ledigestillinger.test

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.serde.AdAvroSerializer
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.UUIDSerializer
import java.util.*

private val logger = buildNamedLogger("test.kafka")

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig.pamStillingerKafkaConsumer) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val kafkaProducer = kafkaFactory.createProducer<UUID, Ad>(
            clientId = "${groupId}-producer",
            keySerializer = UUIDSerializer::class,
            valueSerializer = AdAvroSerializer::class
        )

        val ad1 = TestData.ad()
        val records: List<ProducerRecord<UUID, Ad>> = listOf(
            ProducerRecord(topic, ad1.first, ad1.second),
        )

        try {
            records.forEach { record ->
                logger.info("Sender key {} value {}", record.key(), record.value())
                kafkaProducer.send(record).get()
            }
        } catch (e: Exception) {
            logger.error("Send melding feilet", e)
        } finally {
            kafkaProducer.close()
        }
    }
}
