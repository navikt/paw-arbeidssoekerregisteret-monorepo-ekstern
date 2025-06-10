package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import org.apache.kafka.streams.StreamsBuilder

private val logger = buildApplicationLogger

fun StreamsBuilder.buildBeriket14aVedtakStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
) {
    logger.info("Oppretter KStream for beriket 14a-vedtak")
    val kafkaTopology = applicationConfig.kafkaTopology
    /*
    this.stream(
        kafkaTopology.beriket14aVedtakTopic, Consumed.with(Serdes.Long(), buildBeriket14aVedtakSerde())
    ).peek { key, _ ->
        logger.debug("Mottok event på {} med key {}", kafkaTopology.beriket14aVedtakTopic, key)
        meterRegistry.tellAntallMottatteBeriket14aVedtak()
    }.genericProcess<Long, Beriket14aVedtak, Long, Toggle>(
        name = "handtereToggleForBeriket14aVedtak",
        stateStoreNames = arrayOf(kafkaTopology.periodeStoreName)
    ) { record ->
        processBeriket14aVedtak(applicationConfig, meterRegistry, record)
    }*/
}

/*
@WithSpan(value = "beriket_14a_vedtak_toggle_processor")
private fun ProcessorContext<Long, Toggle>.processBeriket14aVedtak(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    record: Record<Long, Beriket14aVedtak>
) {
    val beriket14aVedtak = record.value()
    val toggleSource = ToggleSource.SISTE_14A_VEDTAK

    val kafkaTopology = applicationConfig.kafkaTopology
    val microfrontendToggle = applicationConfig.microfrontendToggle

    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopology.periodeStoreName)
    val periodeInfo = stateStore.get(beriket14aVedtak.arbeidssoekerId)

    // Sjekk om vedtak er innenfor en aktiv periode
    if (periodeInfo == null) {
        logger.debug("Det ble gjort et 14a vedtak, men fant ingen tilhørende arbeidsøkerperiode")
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggle.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "mangler_periode"
        )
    } else if (periodeInfo.erAvsluttet()) {
        logger.debug("Det ble gjort et 14a vedtak, men tilhørende arbeidsøkerperiode er avsluttet")
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggle.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "avsluttet_periode"
        )
    } else if (periodeInfo.erInnenfor(beriket14aVedtak.fattetDato)) {
        // Send event for å deaktivere AIA Behovsvurdering
        val disableAiaBehovsvurderingToggle = iverksettDeaktiverToggle(
            periodeInfo,
            microfrontendToggle.aiaBehovsvurdering,
            toggleSource
        )
        // Registrer metrikk for toggle
        meterRegistry.tellAntallSendteToggles(
            disableAiaBehovsvurderingToggle,
            toggleSource,
            "vedtak_for_aktiv_periode"
        )
    } else {
        logger.debug(
            "Det ble gjort et 14a vedtak, men vedtakstidspunkt er ikke innenfor aktiv arbeidsøkerperiode {}",
            periodeInfo.id
        )
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggle.aiaBehovsvurdering,
            toggleSource,
            ToggleAction.DISABLE,
            "vedtak_utenfor_periode"
        )
    }
}

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.iverksettDeaktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontendId: String,
    toggleSource: ToggleSource
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.DISABLE.value)
    currentSpan.setAttribute("target", microfrontendId)
    currentSpan.setAttribute("source", toggleSource.value)
    logger.debug(
        "Det ble gjort et 14a vedtak for aktiv arbeidsøkerperiode {}. Iverksetter deaktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val disableToggle = periodeInfo.buildDisableToggle(microfrontendId)
    forward(disableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}
*/
