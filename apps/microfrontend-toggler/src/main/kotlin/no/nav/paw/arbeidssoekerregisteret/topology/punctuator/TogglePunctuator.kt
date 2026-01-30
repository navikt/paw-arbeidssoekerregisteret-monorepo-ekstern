package no.nav.paw.arbeidssoekerregisteret.topology.punctuator

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleSource
import no.nav.paw.arbeidssoekerregisteret.topology.processor.iverksettDeaktiverToggle
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredeAktivePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredeAvsluttedePerioder
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallLagredePerioderTotalt
import no.nav.paw.arbeidssoekerregisteret.utils.tellAntallSendteToggles
import no.nav.paw.kafka.processor.Punctuation
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class TogglePunctuator(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry
) {
    private val logger = buildApplicationLogger

    @WithSpan(value = "periode_toggle_punctuator", kind = SpanKind.INTERNAL)
    fun punctuate(timestamp: Instant, context: ProcessorContext<Long, Toggle>) {
        with(context) {
            logger.info("Starter skedulert jobb for forsinket deaktivering")

            val toggleSource = ToggleSource.ARBEIDSSOEKERPERIODE

            val kafkaTopologyConfig = applicationConfig.kafkaTopology
            val microfrontendToggleConfig = applicationConfig.microfrontendToggle

            val antallTotalt = AtomicLong(0)
            val antallAktive = AtomicLong(0)
            val antallAvsluttede = AtomicLong(0)

            val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaTopologyConfig.periodeStateStore)
            stateStore.all().asSequence().forEach { keyValue ->
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
                    logger.info(
                        "Forsinket deaktivering for avsluttet arbeidsøkerperiode er utløpt, utfører deaktivering av {}",
                        microfrontendToggleConfig.aiaMinSide
                    )

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

            logger.info(
                "Fullførte skedulert jobb for forsinket deaktivering, deaktiverte {} av {}",
                antallAvsluttede,
                antallTotalt
            )
        }
    }

    companion object {
        fun build(
            applicationConfig: ApplicationConfig,
            meterRegistry: MeterRegistry
        ): Punctuation<Long, Toggle> {
            val togglePunctuator = TogglePunctuator(applicationConfig, meterRegistry)
            return Punctuation(
                applicationConfig.microfrontendToggle.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
            ) { timestamp, context ->
                togglePunctuator.punctuate(timestamp, context)
            }
        }
    }
}