package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallLagredeAktivePerioder
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallLagredeAvsluttedePerioder
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallLagredePerioderTotalt
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallMottattePerioder
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallSendteToggles
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
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
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapNonNull
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

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildPeriodeKStream(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    logger.info("Oppretter KStream for arbeidssøkerperioder")
    val kafkaStreamsConfig = appConfig.kafkaStreams

    this.stream<Long, Periode>(kafkaStreamsConfig.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaStreamsConfig.periodeTopic, key)
            meterRegistry.tellAntallMottattePerioder()
        }.mapNonNull("mapTilPeriodeInfo") { periode ->
            hentKafkaKeys(periode.identitetsnummer)?.let { periode.buildPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaStreamsConfig.periodeStoreName),
            punctuation = buildPunctuation(meterRegistry)
        ) { record ->
            processPeriode(meterRegistry, record)
        }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}

context(ConfigContext, LoggingContext)
@WithSpan(value = "periode_toggle_processor", kind = SpanKind.INTERNAL)
private fun ProcessorContext<Long, Toggle>.processPeriode(
    meterRegistry: MeterRegistry,
    record: Record<Long, PeriodeInfo>
) {
    val periodeInfo = record.value()
    val toggleSource = ToggleSource.ARBEIDSSOEKERPERIODE

    val kafkaStreamsConfig = appConfig.kafkaStreams
    val reglerConfig = appConfig.regler
    val microfrontendConfig = appConfig.microfrontends

    val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)

    when {
        periodeInfo.erAvsluttet() -> { // Avsluttet periode
            val utsattDeaktiveringsfrist = Instant.now().minus(reglerConfig.utsattDeaktiveringAvAiaMinSide)
            if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
                // Send event for å deaktiver AIA Min Side
                val disableAiaMinSideToggle = iverksettDeaktiverToggle(
                    periodeInfo,
                    microfrontendConfig.aiaMinSide,
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
                logger.info(
                    "Arbeidsøkerperiode {} er avluttet. Lagrer forsinket deaktivering av {}.",
                    periodeInfo.id,
                    microfrontendConfig.aiaMinSide
                )
                // Lagre periode i state store
                stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)
            }

            // Send event for å deaktivere AIA Behovsvurdering
            val disableAiaBehovsvurderingToggle = iverksettDeaktiverToggle(
                periodeInfo,
                microfrontendConfig.aiaBehovsvurdering,
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
                microfrontendConfig.aiaMinSide,
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
                microfrontendConfig.aiaBehovsvurdering,
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

context(ConfigContext, LoggingContext)
private fun buildPunctuation(meterRegistry: MeterRegistry): Punctuation<Long, Toggle> {
    val togglePunctuation = TogglePunctuation(meterRegistry)
    return Punctuation(
        appConfig.regler.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        togglePunctuation.punctuate(timestamp, context)
    }
}

class TogglePunctuation(private val meterRegistry: MeterRegistry) {

    context(ConfigContext, LoggingContext)
    @WithSpan(value = "periode_toggle_punctuator", kind = SpanKind.INTERNAL)
    fun punctuate(timestamp: Instant, context: ProcessorContext<Long, Toggle>) {
        with(context) {
            val toggleSource = ToggleSource.ARBEIDSSOEKERPERIODE

            val kafkaStreamsConfig = appConfig.kafkaStreams
            val reglerConfig = appConfig.regler
            val microfrontendConfig = appConfig.microfrontends

            val antallTotalt = AtomicLong(0)
            val antallAktive = AtomicLong(0)
            val antallAvsluttede = AtomicLong(0)

            val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
            for (keyValue in stateStore.all()) {
                val periodeInfo = keyValue.value

                antallTotalt.incrementAndGet()
                if (periodeInfo.erAvsluttet()) {
                    antallAvsluttede.incrementAndGet()
                } else {
                    antallAktive.incrementAndGet()
                }

                // Om det er gått mer en 21 dager fra perioden ble avsluttet
                val utsattDeaktiveringsfrist = timestamp.minus(reglerConfig.utsattDeaktiveringAvAiaMinSide)
                if (periodeInfo.bleAvsluttetTidligereEnn(utsattDeaktiveringsfrist)) {
                    // Send event for å deaktiver AIA Min Side
                    val disableAiaMinSideToggle = iverksettDeaktiverToggle(
                        periodeInfo,
                        microfrontendConfig.aiaMinSide,
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

context(ConfigContext, LoggingContext)
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
    logger.info(
        "Arbeidsøkerperiode {} er aktiv. Iverksetter aktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val enableToggle = periodeInfo.buildEnableToggle(microfrontendId)
    forward(enableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return enableToggle
}

context(ConfigContext, LoggingContext)
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
    logger.info(
        "Arbeidsøkerperiode {} er avluttet og utsettelsestid er utløpt. Iverksetter deaktivering av {}.",
        periodeInfo.id,
        microfrontendId
    )
    val disableToggle = periodeInfo.buildDisableToggle(microfrontendId)
    forward(disableToggle.buildRecord(periodeInfo.arbeidssoekerId))
    return disableToggle
}
