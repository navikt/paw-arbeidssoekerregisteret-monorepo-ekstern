package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.MicroFrontend
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettAktiverToggle
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettDeaktiverToggle
import no.nav.paw.arbeidssoekerregisteret.topology.punctuator.TogglePunctuator
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallIkkeSendteToggles
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallMottattePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallSendteToggles
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant

private val logger = buildApplicationLogger

fun StreamsBuilder.addPeriodeStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
) {
    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    logger.info("Oppretter KStream for {}", kafkaTopologyConfig.periodeTopic)

    this.stream<Long, Periode>(kafkaTopologyConfig.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaTopologyConfig.periodeTopic, key)
            meterRegistry.tellAntallMottattePerioder()
        }.mapValues { periode ->
            kafkaKeysFunction(periode.identitetsnummer).let { periode.asPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaTopologyConfig.periodeStateStore),
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
    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStateStore)
    val eksisterendePeriodeInfo = stateStore.get(periodeInfo.arbeidssoekerId)

    if (eksisterendePeriodeInfo != null && eksisterendePeriodeInfo.id == periodeInfo.id) {
        logger.info("Mottok arbeidsøkerperiode som allerede er mottatt, behandles om duplikat")

        meterRegistry.tellAntallIkkeSendteToggles(
            MicroFrontend.AIA_MIN_SIDE,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            ToggleAction.ENABLE,
            "duplikat_periode"
        )
    } else {
        logger.info("Mottok startet arbeidsøkerperiode, utfører aktivering av {}", MicroFrontend.AIA_MIN_SIDE)

        // Lagre periode i state store
        stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)

        // Send event for å aktivere AIA Min Side
        val enableAiaMinSideToggle = iverksettAktiverToggle(
            periodeInfo,
            MicroFrontend.AIA_MIN_SIDE,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            Sensitivitet.SUBSTANTIAL
        )
        meterRegistry.tellAntallSendteToggles(
            enableAiaMinSideToggle,
            ToggleSource.ARBEIDSSOEKERPERIODE,
            "aktiv_periode"
        )
    }
}

private fun ProcessorContext<Long, Toggle>.processAvsluttetPeriode(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    periodeInfo: PeriodeInfo
) {
    val kafkaTopologyConfig = applicationConfig.kafkaTopology
    val microfrontendToggleConfig = applicationConfig.microfrontendToggle
    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStateStore)

    val utsattDeaktiveringsfrist = Instant.now().minus(microfrontendToggleConfig.utsattDeaktiveringAvAiaMinSide)
    if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
        logger.info(
            "Mottok avsluttet arbeidsøkerperiode eldre enn {}, utfører deaktivering av {}",
            microfrontendToggleConfig.utsattDeaktiveringAvAiaMinSide,
            MicroFrontend.AIA_MIN_SIDE
        )

        // Send event for å deaktiver AIA Min Side
        val disableAiaMinSideToggle = iverksettDeaktiverToggle(
            periodeInfo,
            MicroFrontend.AIA_MIN_SIDE,
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
            "Mottok avsluttet arbeidsøkerperiode yngre enn {}, lagrer forsinket deaktivering av {}",
            microfrontendToggleConfig.utsattDeaktiveringAvAiaMinSide,
            MicroFrontend.AIA_MIN_SIDE
        )
        // Lagre periode i state store
        stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)
    }
}

