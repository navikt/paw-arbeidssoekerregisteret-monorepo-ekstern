package no.nav.paw.rapportering.api.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.rapportering.api.ApplicationInfo
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

class RapporteringProducer(
    private val kafkaConfig: KafkaConfig,
    private val applicationConfig: ApplicationConfig,
) {
    private lateinit var producer: Producer<Long, Melding>
    private val meldingSerde = SpecificAvroSerde<Melding>().apply {
        configure(mapOf("schema.registry.url" to kafkaConfig.schemaRegistry), false)
    }

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer =
            kafkaFactory.createProducer<Long, Melding>(
                clientId = applicationConfig.applicationIdSuffix,
                keySerializer = LongSerializer::class,
                valueSerializer = meldingSerde.serializer()::class
            )
    }

    fun produceMessage(key: Long, message: Melding) {
        producer.send(ProducerRecord(applicationConfig.rapporteringTopic, key, message))
    }

    fun closeProducer() {
        producer.close()
    }
}

fun createMelding(state: RapporteringTilgjengelig, rapportering: RapporteringRequest) = Melding.newBuilder()
    .setId(ApplicationInfo.id)
    .setNamespace("paw")
    .setPeriodeId(state.periodeId)
    .setGjelderFra(state.gjelderFra)
    .setGjelderTil(state.gjelderTil)
    .setVilFortsetteSomArbeidssoeker(rapportering.vilFortsetteSomArbeidssoeker)
    .setHarJobbetIDennePerioden(rapportering.harJobbetIDennePerioden)
    .build()
