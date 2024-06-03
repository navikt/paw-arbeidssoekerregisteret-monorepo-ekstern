package no.nav.paw.arbeidssoekerregisteret.topology

import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.PdlClient
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildSiste14aVedtakTopology(kafkaKeysClient: KafkaKeysClient, pdlClient: PdlClient) {
    val kafkaStreamsConfig = appConfig.kafkaStreams

    this.stream(
        kafkaStreamsConfig.siste14aVedtakTopic,
        Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).genericProcess<String, Siste14aVedtak, Long, Toggle>(
        kafkaStreamsConfig.siste41aVedtakToggleProcessor,
        kafkaStreamsConfig.periodeStoreName
    ) { record ->
        val siste14aVedtak = record.value()
        // 1. Motta vedtak event
        // 2. Sjekk om vedtak er innenfor en aktiv periode
        // 3. Hent fnr for aktørId fra PDL
        // 4. Hent arbeidssøkerId for fnr fra KafkaKeys
        // 5. Produsere toggle event
        val keyValueStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
    }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}