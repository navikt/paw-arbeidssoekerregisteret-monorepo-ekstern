package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import no.nav.paw.arbeidssoekerregisteret.model.bleAvsluttetTidligereEnn
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.buildRecord
import no.nav.paw.arbeidssoekerregisteret.model.erAvsluttet
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredeAktivePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredeAvsluttedePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredePerioderTotalt
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallMottattePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallSendteToggles
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

private val logger = buildApplicationLogger

fun StreamsBuilder.buildPeriodeStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
) {
    logger.info("Oppretter KStream for arbeidssøkerperioder")
    val kafkaTopology = applicationConfig.kafkaTopology

    this.stream<Long, Periode>(kafkaTopology.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaTopology.periodeTopic, key)
            meterRegistry.tellAntallMottattePerioder()
        }.mapValues { periode ->
            kafkaKeysFunction(periode.identitetsnummer).let { periode.buildPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaTopology.periodeStoreName),
            punctuation = buildPunctuation(applicationConfig, meterRegistry)
        ) { record ->
            processPeriode(applicationConfig, meterRegistry, record)
        }.to(kafkaTopology.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}

@WithSpan(value = "periode_toggle_processor", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.processPeriode(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    record: Record<Long, PeriodeInfo>
) {
    val periodeInfo = record.value()
    val toggleSource = ToggleSource.ARBEIDSSOEKERPERIODE

    val kafkaTopology = applicationConfig.kafkaTopology
    val microfrontendToggle = applicationConfig.microfrontendToggle

    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopology.periodeStoreName)

    when {
        periodeInfo.erAvsluttet() -> { // Avsluttet periode
            val utsattDeaktiveringsfrist = Instant.now().minus(microfrontendToggle.utsattDeaktiveringAvAiaMinSide)
            if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
                // Send event for å deaktiver AIA Min Side
                val disableAiaMinSideToggle = iverksettDeaktiverToggle(
                    periodeInfo,
                    microfrontendToggle.aiaMinSide,
                    toggleSource
                )
                // Slett periode fra state store
                stateStore.delete(periodeInfo.arbeidssoekerId)
                // Registrer metrikk for toggle
                meterRegistry.tellAntallSendteToggles(
                    disableAiaMinSideToggle,
                    toggleSource,
                    "avsluttet_periode"
                )
            } else {
                logger.debug(
                    "Arbeidsøkerperiode {} er avluttet. Lagrer forsinket deaktivering av {}.",
                    periodeInfo.id,
                    microfrontendToggle.aiaMinSide
                )
                // Lagre periode i state store
                stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)
            }

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
                "avsluttet_periode"
            )
        }

        else -> {
            // Lagre periode i state store
            stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)

            // Send event for å aktivere AIA Min Side
            val enableAiaMinSideToggle = iverksettAktiverToggle(
                periodeInfo,
                microfrontendToggle.aiaMinSide,
                toggleSource
            )
            meterRegistry.tellAntallSendteToggles(
                enableAiaMinSideToggle,
                toggleSource,
                "aktiv_periode"
            )

            // Send event for å aktivere AIA Behovsvurdering
            val enableAiaBehovsvurderingToggle = iverksettAktiverToggle(
                periodeInfo,
                microfrontendToggle.aiaBehovsvurdering,
                toggleSource
            )
            meterRegistry.tellAntallSendteToggles(
                enableAiaBehovsvurderingToggle,
                toggleSource,
                "aktiv_periode"
            )
        }
    }
}

private fun buildPunctuation(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry
): Punctuation<Long, Toggle> {
    val togglePunctuation = TogglePunctuation(applicationConfig, meterRegistry)
    return Punctuation(
        applicationConfig.microfrontendToggle.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        togglePunctuation.punctuate(timestamp, context)
    }
}

class TogglePunctuation(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry
) {

    @WithSpan(value = "periode_toggle_punctuator", kind = SpanKind.INTERNAL)
    fun punctuate(timestamp: Instant, context: ProcessorContext<Long, Toggle>) {
        with(context) {
            val toggleSource = ToggleSource.ARBEIDSSOEKERPERIODE

            val kafkaTopologyConfig = applicationConfig.kafkaTopology
            val microfrontendToggleConfig = applicationConfig.microfrontendToggle

            val antallTotalt = AtomicLong(0)
            val antallAktive = AtomicLong(0)
            val antallAvsluttede = AtomicLong(0)

            val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStoreName)
            for (keyValue in stateStore.all()) {
                val periodeInfo = keyValue.value

                antallTotalt.incrementAndGet()
                if (periodeInfo.erAvsluttet()) {
                    antallAvsluttede.incrementAndGet()
                } else {
                    antallAktive.incrementAndGet()
                }

                // Om det er gått mer en 21 dager fra perioden ble avsluttet
                val utsattDeaktiveringsfrist = timestamp.minus(microfrontendToggleConfig.utsattDeaktiveringAvAiaMinSide)
                if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
                    // Send event for å deaktiver AIA Min Side
                    val disableAiaMinSideToggle = iverksettDeaktiverToggle(
                        periodeInfo,
                        microfrontendToggleConfig.aiaMinSide,
                        toggleSource
                    )
                    // Slett periode fra state store
                    stateStore.delete(periodeInfo.arbeidssoekerId)
                    // Registrer metrikk for toggle
                    meterRegistry.tellAntallSendteToggles(
                        disableAiaMinSideToggle,
                        toggleSource,
                        "avsluttet_periode"
                    )
                }
            }

            meterRegistry.tellAntallLagredePerioderTotalt(antallTotalt)
            meterRegistry.tellAntallLagredeAktivePerioder(antallAktive)
            meterRegistry.tellAntallLagredeAvsluttedePerioder(antallAvsluttede)
        }
    }
}

@WithSpan(value = "microfrontend_toggle", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.iverksettAktiverToggle(
    periodeInfo: PeriodeInfo,
    microfrontendId: String,
    toggleSource: ToggleSource
): Toggle {
    val currentSpan = Span.current()
    currentSpan.setAttribute("action", ToggleAction.ENABLE.value)
    currentSpan.setAttribute("target", microfrontendId)
    currentSpan.setAttribute("source", toggleSource.value)
    logger.debug(
        "Arbeidsøkerperiode {} er aktiv. Iverksetter aktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val enableToggle = periodeInfo.buildEnableToggle(microfrontendId)
    forward(enableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return enableToggle
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
        "Arbeidsøkerperiode {} er avluttet og utsettelsestid er utløpt. Iverksetter deaktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val disableToggle = periodeInfo.buildDisableToggle(microfrontendId)
    forward(disableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}
