package no.nav.paw.arbeidssoekerregisteret.topology.streams

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import no.nav.paw.kafka.processor.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.processor.api.Record

private val logger = buildApplicationLogger

fun StreamsBuilder.buildEgenvurderingStream(
    applicationConfig: ApplicationConfig,
    kafkaStreamsFactory: KafkaStreamsFactory,
) {
    logger.info("Oppretter KStream for periode")
    val kafkaTopology = applicationConfig.kafkaTopology
    this.stream(
        kafkaTopology.egenvurderingTopic, Consumed.with(Serdes.Long(), kafkaStreamsFactory.createSpecificAvroSerde<Egenvurdering>())
    ).peek { key, _ ->
        logger.debug("Mottok event på ${kafkaTopology.periodeTopic} med key $key")
    }.genericProcess<Long, Egenvurdering, Long, Egenvurdering>(
        name = "handtereToggleForPeriode",
        stateStoreNames = arrayOf(kafkaTopology.egenvurderingStateStoreName)
    ) { record ->
        processProfilering(applicationConfig, record)
    }
}

fun processProfilering(
    applicationConfig: ApplicationConfig,
    record: Record<Long, Egenvurdering>
): Record<Long, Egenvurdering> {
    logger.info("Behandler periode med key ${record.key()} og value ${record.value()}")
    return record
}