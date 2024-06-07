package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallToggles
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtakInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildRecord
import no.nav.paw.arbeidssoekerregisteret.model.erAvsluttet
import no.nav.paw.arbeidssoekerregisteret.model.erInnenfor
import no.nav.paw.arbeidssoekerregisteret.model.tilSiste14aVedtakInfo
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapKeyAndValue
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.state.KeyValueStore

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildSiste14aVedtakTopology(
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    val kafkaStreamsConfig = appConfig.kafkaStreams
    val microfrontendConfig = appConfig.microfrontends

    this.stream(
        kafkaStreamsConfig.siste14aVedtakTopic, Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på ${kafkaStreamsConfig.siste14aVedtakTopic} med key $key")
    }.mapKeyAndValue("mapKeyTilKafkaKeys") { _, siste14aVedtak ->
        hentKafkaKeys(siste14aVedtak.aktorId.get())
            ?.let { it.key to siste14aVedtak.tilSiste14aVedtakInfo(it.id) }
    }.repartition(
        Repartitioned.numberOfPartitions<Long, Siste14aVedtakInfo>(appConfig.kafkaStreams.siste14aVedtakPartitionCount)
            .withName("repartition14aVedtak")
            .withKeySerde(Serdes.Long()).withValueSerde(buildSiste14aVedtakInfoSerde())
    ).genericProcess<Long, Siste14aVedtakInfo, Long, Toggle>(
        name = "handtereToggleFor14aVedtak", stateStoreNames = arrayOf(kafkaStreamsConfig.periodeStoreName)
    ) { record ->
        val siste14aVedtakInfo = record.value()
        val keyValueStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
        val periodeInfo = keyValueStore.get(siste14aVedtakInfo.arbeidssoekerId)

        // Sjekk om vedtak er innenfor en aktiv periode,
        if (periodeInfo != null && !periodeInfo.erAvsluttet() && periodeInfo.erInnenfor(siste14aVedtakInfo.fattetDato)) {
            logger.info(
                "Det ble gjort et 14a vedtak for arbeidsøkerperiode {}. Iverksetter deaktivering av {}.",
                periodeInfo.id,
                microfrontendConfig.aiaBehovsvurdering
            )
            val disableAiaBehovsvurderingToggle = periodeInfo.buildDisableToggle(microfrontendConfig.aiaBehovsvurdering)
            meterRegistry.tellAntallToggles(disableAiaBehovsvurderingToggle)
            forward(disableAiaBehovsvurderingToggle.buildRecord(siste14aVedtakInfo.arbeidssoekerId))
        }
    }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}