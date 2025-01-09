package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.PeriodeSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)

    produserPeriodeMeldinger(kafkaConfig, applicationConfig)
}

fun produserPeriodeMeldinger(
    kafkaConfig: KafkaConfig,
    applicationConfig: ApplicationConfig
) {
    val localProducer = LocalProducer(kafkaConfig, applicationConfig)
    try {
        PeriodeProducerUtils().lagTestPerioder().forEach { periode ->
            localProducer.producePeriodeMessage(applicationConfig.periodeTopic, 1234L, periode)
        }
    } catch (e: Exception) {
        println("LocalProducer periode error: ${e.message}")
        localProducer.closePeriodeProducer()
    }
}

class LocalProducer(kafkaConfig: KafkaConfig, applicationConfig: ApplicationConfig) {
    private val periodeProducer: Producer<Long, Periode> =
        KafkaFactory(kafkaConfig)
            .createProducer<Long, Periode>(
                clientId = applicationConfig.gruppeId,
                keySerializer = LongSerializer::class,
                valueSerializer = PeriodeSerializer::class
            )

    fun producePeriodeMessage(
        topic: String,
        key: Long,
        value: Periode
    ) {
        val record = ProducerRecord(topic, key, value)
        periodeProducer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send periode message: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }.get()
    }

    fun closePeriodeProducer() {
        periodeProducer.close()
    }
}
