package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallMottatteSiste14aVedtak
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.tilBeriket14aVedtak
import no.nav.paw.config.kafka.streams.mapKeyAndValue
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildSiste14aVedtakKStream(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    logger.info("Aktiverer KStream for berikelse av 14a-vedtak")
    val kafkaStreamsConfig = appConfig.kafkaStreams

    this.stream(
        kafkaStreamsConfig.siste14aVedtakTopic, Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på {} med key {}", kafkaStreamsConfig.siste14aVedtakTopic, key)
        meterRegistry.tellAntallMottatteSiste14aVedtak()
    }.mapKeyAndValue("mapKafkaKeys") { _, siste14aVedtak ->
        hentKafkaKeys(siste14aVedtak.aktorId.get())
            ?.let { it.key to siste14aVedtak.tilBeriket14aVedtak(it.id) }
    }.to(kafkaStreamsConfig.beriket14aVedtakTopic, Produced.with(Serdes.Long(), buildBeriket14aVedtakSerde()))
}
