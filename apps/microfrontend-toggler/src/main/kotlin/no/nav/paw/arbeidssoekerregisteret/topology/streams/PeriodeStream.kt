package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettAktiverToggle
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettDeaktiverToggle
import no.nav.paw.arbeidssoekerregisteret.topology.punctuator.TogglePunctuator
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallIkkeSendteToggles
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallMottattePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallSendteToggles
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant

private val logger = buildApplicationLogger

fun StreamsBuilder.buildPeriodeStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
) {
    logger.info("Oppretter KStream for arbeidssøkerperioder")
    val kafkaTopologyConfig = applicationConfig.kafkaTopology

    this.stream<Long, Periode>(kafkaTopologyConfig.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaTopologyConfig.periodeTopic, key)
            meterRegistry.tellAntallMottattePerioder()
        }.mapValues { periode ->
            kafkaKeysFunction(periode.identitetsnummer).let { periode.asPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaTopologyConfig.periodeStoreName),
            punctuation = TogglePunctuator.build(applicationConfig, meterRegistry)
        ) { record ->
            processPeriode(applicationConfig, meterRegistry, record)
        }.to(kafkaTopologyConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}

@WithSpan(value = "periode_toggle_processor", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.processPeriode(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    record: Record<Long, PeriodeInfo>
) {
    val periodeInfo = record.value()

    if (periodeInfo.erAktiv()) {
        processStartetPeriode(applicationConfig, meterRegistry, periodeInfo)
    } else {
        processAvsluttetPeriode(applicationConfig, meterRegistry, periodeInfo)
    }
}

private fun ProcessorContext<Long, Toggle>.processStartetPeriode(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    periodeInfo: PeriodeInfo
) {
    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    val microfrontendToggleConfig = applicationConfig.microfrontendToggle
    val deprekeringConfig = applicationConfig.deprekering
    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStoreName)
    val eksisterendePeriodeInfo = stateStore.get(periodeInfo.arbeidssoekerId)

    if (eksisterendePeriodeInfo != null && eksisterendePeriodeInfo.id == periodeInfo.id) {
        logger.info("Mottok arbeidsøkerperiode som allerede er mottatt, behandles om duplikat")

        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggleConfig.aiaMinSide,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            ToggleAction.ENABLE,
            "duplikat_periode"
        )
        meterRegistry.tellAntallIkkeSendteToggles(
            microfrontendToggleConfig.aiaBehovsvurdering,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            ToggleAction.ENABLE,
            "duplikat_periode"
        )
    } else {
        logger.info("Mottok startet arbeidsøkerperiode, utfører aktivering av {}", microfrontendToggleConfig.aiaMinSide)

        // Lagre periode i state store
        stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)

        // Send event for å aktivere AIA Min Side
        val enableAiaMinSideToggle = iverksettAktiverToggle(
            periodeInfo,
            microfrontendToggleConfig.aiaMinSide,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            microfrontendToggleConfig.aiaMinSideSensitivitet
        )
        meterRegistry.tellAntallSendteToggles(
            enableAiaMinSideToggle,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            "aktiv_periode"
        )

        if (periodeInfo.startet.isBefore(deprekeringConfig.tidspunkt)) {
            logger.info(
                "Arbeidsøkerperiode startet før deprekeringstidspunkt {}, utfører aktivering av {}",
                deprekeringConfig.tidspunkt,
                microfrontendToggleConfig.aiaBehovsvurdering
            )

            // Send event for å aktivere AIA Behovsvurdering
            val enableAiaBehovsvurderingToggle = iverksettAktiverToggle(
                periodeInfo,
                microfrontendToggleConfig.aiaBehovsvurdering,
                ToggleSource.ARBEIDSSOEKERPERIODE,
                microfrontendToggleConfig.aiaBehovsvurderingSensitivitet
            )
            meterRegistry.tellAntallSendteToggles(
                enableAiaBehovsvurderingToggle,
                ToggleSource.ARBEIDSSOEKERPERIODE,
                "aktiv_periode"
            )
        } else {
            logger.info(
                "Arbeidsøkerperiode startet etter deprekeringstidspunkt {}, så avbryter aktivering av {}",
                deprekeringConfig.tidspunkt,
                microfrontendToggleConfig.aiaBehovsvurdering
            )

            meterRegistry.tellAntallIkkeSendteToggles(
                microfrontendToggleConfig.aiaBehovsvurdering,
                ToggleSource.ARBEIDSSOEKERPERIODE,
                ToggleAction.ENABLE,
                "behovsvurdering_deprekert"
            )
        }
    }
}

private fun ProcessorContext<Long, Toggle>.processAvsluttetPeriode(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    periodeInfo: PeriodeInfo
) {
    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    val microfrontendToggleConfig = applicationConfig.microfrontendToggle
    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStoreName)

    val utsattDeaktiveringsfrist = Instant.now().minus(microfrontendToggleConfig.utsattDeaktiveringAvAiaMinSide)
    if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
        logger.info(
            "Mottok avsluttet arbeidsøkerperiode, utfører deaktivering av {}",
            microfrontendToggleConfig.aiaMinSide
        )

        // Send event for å deaktiver AIA Min Side
        val disableAiaMinSideToggle = iverksettDeaktiverToggle(
            periodeInfo,
            microfrontendToggleConfig.aiaMinSide,
            ToggleSource.ARBEIDSSOEKERPERIODE
        )
        // Slett periode fra state store
        stateStore.delete(periodeInfo.arbeidssoekerId)
        // Registrer metrikk for toggle
        meterRegistry.tellAntallSendteToggles(
            disableAiaMinSideToggle,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            "avsluttet_periode"
        )
    } else {
        logger.info(
            "Mottok avsluttet arbeidsøkerperiode, lagrer forsinket deaktivering av {}",
            microfrontendToggleConfig.aiaMinSide
        )
        // Lagre periode i state store
        stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)
    }

    logger.info(
        "Mottok avsluttet arbeidsøkerperiode, utfører deaktivering av {}",
        microfrontendToggleConfig.aiaBehovsvurdering
    )
    // Send event for å deaktivere AIA Behovsvurdering
    val disableAiaBehovsvurderingToggle = iverksettDeaktiverToggle(
        periodeInfo,
        microfrontendToggleConfig.aiaBehovsvurdering,
        ToggleSource.ARBEIDSSOEKERPERIODE
    )
    // Registrer metrikk for toggle
    meterRegistry.tellAntallSendteToggles(
        disableAiaBehovsvurderingToggle,
        ToggleSource.ARBEIDSSOEKERPERIODE,
        "avsluttet_periode"
    )
}

