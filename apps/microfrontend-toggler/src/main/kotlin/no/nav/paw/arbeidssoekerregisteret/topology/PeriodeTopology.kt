package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallLagredePerioder
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallToggles
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.buildRecord
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapNonNull
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

context(ConfigContext, LoggingContext)
private fun buildPunctuation(meterRegistry: MeterRegistry): Punctuation<Long, Toggle> {
    return Punctuation(
        appConfig.regler.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        val microfrontendConfig = appConfig.microfrontends
        val antallLagredePerioderReference = AtomicLong(0)

        val stateStore: KeyValueStore<Long, PeriodeInfo> =
            context.getStateStore(appConfig.kafkaStreams.periodeStoreName)
        for (keyValue in stateStore.all()) {
            val periodeInfo = keyValue.value

            antallLagredePerioderReference.incrementAndGet()

            if (periodeInfo.avsluttet != null) { // Avsluttet periode
                // Om det er gått mer en utsettelsestid (21 dager) fra perioden ble avsluttet så send event for å
                // deaktivere AIA Min Side
                if (periodeInfo.avsluttet.isBefore(timestamp.minus(appConfig.regler.utsattDeaktiveringAvAiaMinSide))) {
                    logger.info(
                        "Arbeidsøkerperiode {} er avluttet og utsettelsestid er utløpt. Iverksetter forsinket deaktivering av {}.",
                        periodeInfo.id,
                        microfrontendConfig.aiaMinSide
                    )
                    val disableAiaMinSideToggle = periodeInfo.buildDisableToggle(microfrontendConfig.aiaMinSide)
                    meterRegistry.tellAntallToggles(disableAiaMinSideToggle)
                    context.forward(disableAiaMinSideToggle.buildRecord(periodeInfo.arbeidssoekerId))
                    stateStore.delete(periodeInfo.arbeidssoekerId)
                }
            }
        }

        meterRegistry.tellAntallLagredePerioder(antallLagredePerioderReference)
    }
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildPeriodeTopology(
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    val kafkaStreamsConfig = appConfig.kafkaStreams
    val microfrontendConfig = appConfig.microfrontends

    this.stream<Long, Periode>(kafkaStreamsConfig.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaStreamsConfig.periodeTopic, key)
        }.mapNonNull("mapTilPeriodeInfo") { periode ->
            hentKafkaKeys(periode.identitetsnummer)?.let { periode.buildPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaStreamsConfig.periodeStoreName),
            punctuation = buildPunctuation(meterRegistry)
        ) { record ->
            val stateStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
            val periodeInfo = record.value()

            // Lagre perioden i state store
            stateStore.put(periodeInfo.arbeidssoekerId, periodeInfo)

            when {
                periodeInfo.avsluttet != null -> { // Avsluttet periode
                    if (periodeInfo.avsluttet.isBefore(
                            Instant.now().minus(appConfig.regler.utsattDeaktiveringAvAiaMinSide)
                        )
                    ) {
                        logger.info(
                            "Arbeidsøkerperiode {} er avluttet og utsettelsestid er utløpt, Iverksetter deaktivering av {}.",
                            periodeInfo.id,
                            microfrontendConfig.aiaMinSide
                        )
                        val disableAiaMinSideToggle = periodeInfo.buildDisableToggle(microfrontendConfig.aiaMinSide)
                        meterRegistry.tellAntallToggles(disableAiaMinSideToggle)
                        forward(disableAiaMinSideToggle.buildRecord(periodeInfo.arbeidssoekerId))
                        stateStore.delete(periodeInfo.arbeidssoekerId)
                    } else {
                        logger.info(
                            "Arbeidsøkerperiode {} er avluttet. Forsinket deaktivering av {}.",
                            periodeInfo.id,
                            microfrontendConfig.aiaMinSide
                        )
                    }

                    logger.info(
                        "Arbeidsøkerperiode {} er avluttet. Iverksetter deaktivering av {}.",
                        periodeInfo.id,
                        microfrontendConfig.aiaBehovsvurdering
                    )

                    // Send event for å deaktivere AIA Behovsvurdering
                    val disableAiaBehovsvurderingToggle =
                        periodeInfo.buildDisableToggle(microfrontendConfig.aiaBehovsvurdering)
                    meterRegistry.tellAntallToggles(disableAiaBehovsvurderingToggle)
                    forward(disableAiaBehovsvurderingToggle.buildRecord(periodeInfo.arbeidssoekerId))
                }

                else -> {
                    logger.info(
                        "Arbeidsøkerperiode {} er aktiv. Iverksetter aktivering av {} og {}.",
                        periodeInfo.id,
                        microfrontendConfig.aiaMinSide,
                        microfrontendConfig.aiaBehovsvurdering
                    )

                    // Send event for å aktivere AIA Min Side
                    val enableAiaMinSideToggle = periodeInfo.buildEnableToggle(microfrontendConfig.aiaMinSide)
                    meterRegistry.tellAntallToggles(enableAiaMinSideToggle)
                    forward(enableAiaMinSideToggle.buildRecord(periodeInfo.arbeidssoekerId))

                    // Send event for å aktivere AIA Behovsvurdering
                    val enableAiaBehovsvurderingToggle =
                        periodeInfo.buildEnableToggle(microfrontendConfig.aiaBehovsvurdering)
                    meterRegistry.tellAntallToggles(enableAiaBehovsvurderingToggle)
                    forward(enableAiaBehovsvurderingToggle.buildRecord(periodeInfo.arbeidssoekerId))
                }
            }
        }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}
