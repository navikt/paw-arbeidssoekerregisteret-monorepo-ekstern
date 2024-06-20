package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
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
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

context(ConfigContext, LoggingContext)
@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.iverksettDeaktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontendId: String
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", "disable")
    currentSpan.setAttribute("microfrontend_id", microfrontendId)
    logger.info(
        "Det ble gjort et 14a vedtak for aktiv arbeidsøkerperiode {}. Iverksetter deaktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val disableToggle = periodeInfo.buildDisableToggle(microfrontendId)
    forward(disableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}

context(ConfigContext, LoggingContext)
@WithSpan(value = "siste_14a_vedtak_toggle_processor")
private fun ProcessorContext<Long, Toggle>.processSiste14aVedtak(
    meterRegistry: MeterRegistry,
    record: Record<Long, Siste14aVedtakInfo>
) {
    val siste14aVedtakInfo = record.value()

    val kafkaStreamsConfig = appConfig.kafkaStreams
    val microfrontendConfig = appConfig.microfrontends

    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
    val periodeInfo = stateStore.get(siste14aVedtakInfo.arbeidssoekerId)

    // Sjekk om vedtak er innenfor en aktiv periode
    if (periodeInfo == null) {
        logger.warn("Det ble gjort et 14a vedtak, men fant ingen tilhørende arbeidsøkerperiode")
    } else if (periodeInfo.erAvsluttet()) {
        logger.info("Det ble gjort et 14a vedtak, men tilhørende arbeidsøkerperiode er avsluttet")
    } else if (periodeInfo.erInnenfor(siste14aVedtakInfo.fattetDato)) {
        // Send event for å deaktivere AIA Behovsvurdering
        val disableAiaBehovsvurderingToggle =
            iverksettDeaktiverToggle(periodeInfo, microfrontendConfig.aiaBehovsvurdering)
        // Registrer metrikk for toggle
        meterRegistry.tellAntallToggles(disableAiaBehovsvurderingToggle)
    } else {
        logger.warn(
            "Det ble gjort et 14a vedtak, men vedtakstidspunkt er ikke innenfor aktiv arbeidsøkerperiode {}",
            periodeInfo.id
        )
    }
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildSiste14aVedtakTopology(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    val kafkaStreamsConfig = appConfig.kafkaStreams

    this.stream(
        kafkaStreamsConfig.siste14aVedtakTopic, Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på {} med key {}", kafkaStreamsConfig.siste14aVedtakTopic, key)
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
        processSiste14aVedtak(meterRegistry, record)
    }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}