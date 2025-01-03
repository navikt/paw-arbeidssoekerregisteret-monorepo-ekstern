package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.PeriodeSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.common.serialization.LongSerializer

fun main() {
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

    val periodeKafkaProducer = KafkaFactory(kafkaConfig).createProducer<Long, Periode>(
        clientId = applicationConfig.gruppeId,
        keySerializer = LongSerializer::class,
        valueSerializer = PeriodeSerializer::class
    )

    periodeKafkaProducer.use { producer ->
        val topic = applicationConfig.periodeTopic
        val record = TestData.nyProducerRecord(topic = topic)
        producer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send periode message: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }.get()
    }
}
