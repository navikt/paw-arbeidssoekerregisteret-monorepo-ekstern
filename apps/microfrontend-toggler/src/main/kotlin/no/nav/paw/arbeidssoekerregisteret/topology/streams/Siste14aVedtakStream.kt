package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.tilBeriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.utils.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallMottatteSiste14aVedtak
import no.nav.paw.kafka.processor.mapKeyAndValue
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

private val logger = buildApplicationLogger

fun StreamsBuilder.buildSiste14aVedtakStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse?
) {
    logger.info("Oppretter KStream for berikelse av 14a-vedtak")
    val kafkaTopology = applicationConfig.kafkaTopology

    this.stream(
        kafkaTopology.siste14aVedtakTopic, Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på {} med key {}", kafkaTopology.siste14aVedtakTopic, key)
        meterRegistry.tellAntallMottatteSiste14aVedtak()
    }.mapKeyAndValue("mapKafkaKeys") { _, siste14aVedtak ->
        kafkaKeysFunction(siste14aVedtak.aktorId.get())?.let { it.key to siste14aVedtak.tilBeriket14aVedtak(it.id) }
    }.to(kafkaTopology.beriket14aVedtakTopic, Produced.with(Serdes.Long(), buildBeriket14aVedtakSerde()))
}
