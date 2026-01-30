package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettDeaktiverToggle
import no.nav.paw.arbeidssoekerregisteret.topology.punctuator.buildDeprekeringPunctuator
import no.nav.paw.arbeidssoekerregisteret.utils.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallIkkeSendteToggles
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallMottatteBeriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallSendteToggles
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

private val logger = buildApplicationLogger

fun StreamsBuilder.addBeriket14aVedtakStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
) {
    val deprekeringConfig = applicationConfig.deprekering
    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    logger.info("Oppretter KStream for {}", kafkaTopologyConfig.beriket14aVedtakTopic)

    this.stream(
        kafkaTopologyConfig.beriket14aVedtakTopic, Consumed.with(Serdes.Long(), buildBeriket14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på {} med key {}", kafkaTopologyConfig.beriket14aVedtakTopic, key)
        meterRegistry.tellAntallMottatteBeriket14aVedtak()
    }.genericProcess<Long, Beriket14aVedtak, Long, Toggle>(
        name = "handtereToggleForBeriket14aVedtak",
        stateStoreNames = arrayOf(kafkaTopologyConfig.periodeStateStore, deprekeringConfig.stateStore),
        punctuation = buildDeprekeringPunctuator(applicationConfig, kafkaKeysFunction)
    ) { record ->
        processBeriket14aVedtak(applicationConfig, meterRegistry, record)
    }.to(kafkaTopologyConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}

@WithSpan(value = "beriket_14a_vedtak_toggle_processor")
private fun ProcessorContext<Long, Toggle>.processBeriket14aVedtak(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    record: Record<Long, Beriket14aVedtak>
) {
    val beriket14aVedtak = record.value()
    val toggleSource = ToggleSource.SISTE_14A_VEDTAK

    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    val microfrontendToggleConfig = applicationConfig.microfrontendToggle

    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStateStore)
    val periodeInfo = stateStore.get(beriket14aVedtak.arbeidssoekerId)

    // Sjekk om vedtak er innenfor en aktiv periode
    if (periodeInfo == null) {
        logger.info("Mottok 14a-vedtak, men fant ingen tilhørende arbeidsøkerperiode")
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggleConfig.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "mangler_periode"
        )
    } else if (periodeInfo.erAvsluttet()) {
        logger.info("Mottok 14a-vedtak, men tilhørende arbeidsøkerperiode er avsluttet")
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggleConfig.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "avsluttet_periode"
        )
    } else if (periodeInfo.erStartetEtter(beriket14aVedtak.fattetDato)) {
        logger.info("Mottok 14a-vedtak, men vedtakstidspunkt er før start av arbeidsøkerperiode")
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggleConfig.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "vedtak_utenfor_periode"
        )
    } else {
        logger.info(
            "Mottok 14a-vedtak for aktiv arbeidsøkerperiode, utfører deaktivering av {}",
            microfrontendToggleConfig.aiaBehovsvurdering
        )

        // Send event for å deaktivere AIA Behovsvurdering
        val disableAiaBehovsvurderingToggle = iverksettDeaktiverToggle(
            periodeInfo,
            microfrontendToggleConfig.aiaBehovsvurdering,
            toggleSource
        )
        // Registrer metrikk for toggle
        meterRegistry.tellAntallSendteToggles(
            disableAiaBehovsvurderingToggle,
            toggleSource,
            "vedtak_for_aktiv_periode"
        )
    }
}
