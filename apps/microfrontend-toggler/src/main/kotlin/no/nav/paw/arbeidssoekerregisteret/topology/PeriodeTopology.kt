package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallToggles
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
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
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration
import java.time.Instant

context(ConfigContext, LoggingContext)
private fun buildPunctuation(meterRegistry: PrometheusMeterRegistry): Punctuation<Long, Toggle> {
    return Punctuation(
        appConfig.regler.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        val stateStore: KeyValueStore<Long, PeriodeInfo> =
            context.getStateStore(appConfig.kafkaStreams.periodeStoreName)
        for (keyValue in stateStore.all()) {
            val periodeInfo = keyValue.value

            if (periodeInfo.erAvsluttet()) {
                // Om det er gått mer en utsettelsestid (21 dager) fra perioden ble avsluttet så send event for å
                // deaktivere AIA Min Side
                if (timestamp.minus(appConfig.regler.utsattDeaktiveringAvAiaMinSide).isAfter(periodeInfo.avsluttet)) {
                    logger.info(
                        "Utsettelsestid for arbeidsøkerperiode {} er utløpt. Iverksetter forsinket deaktivering av {}.",
                        periodeInfo.id,
                        appConfig.microfrontends.aiaMinSide
                    )
                    stateStore.delete(periodeInfo.arbeidssoekerId)
                    val disableAiaBehovsvurderingToggle =
                        periodeInfo.buildDisableToggle(appConfig.microfrontends.aiaMinSide)
                    meterRegistry.tellAntallToggles(disableAiaBehovsvurderingToggle)
                    context.forward(disableAiaBehovsvurderingToggle.buildRecord(periodeInfo.arbeidssoekerId))
                }
            }
        }
    }
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildPeriodeTopology(
    meterRegistry: PrometheusMeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
) {
    val kafkaStreamsConfig = appConfig.kafkaStreams
    val microfrontendConfig = appConfig.microfrontends

    this.stream<Long, Periode>(kafkaStreamsConfig.periodeTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på ${kafkaStreamsConfig.periodeTopic} med key $key")
        }.filter { _, periode ->
            // Filtrerer vekk perioder som er avsluttet og eldre en 26 dager
            periode.avsluttet == null || periode.avsluttet.tidspunkt.isAfter(
                Instant.now().minus(appConfig.regler.utsattDeaktiveringAvAiaMinSide).minus(Duration.ofDays(5))
            )
        }.mapNonNull("mapTilPeriodeInfo") { periode ->
            hentKafkaKeys(periode.identitetsnummer)?.let { periode.buildPeriodeInfo(it.id) }
        }.genericProcess<Long, PeriodeInfo, Long, Toggle>(
            name = "handtereToggleForPeriode",
            stateStoreNames = arrayOf(kafkaStreamsConfig.periodeStoreName),
            punctuation = buildPunctuation(meterRegistry)
        ) { record ->
            val keyValueStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
            val periodeInfo = record.value()

            // Lagre perioden i state store
            keyValueStore.put(periodeInfo.arbeidssoekerId, periodeInfo)

            when {
                periodeInfo.erAvsluttet() -> {
                    logger.info(
                        "Arbeidsøkerperiode {} ble avluttet. Iverksetter deaktivering av {}. Forsinket deaktivering av {}.",
                        periodeInfo.id,
                        microfrontendConfig.aiaBehovsvurdering,
                        microfrontendConfig.aiaMinSide
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
                    // TODO Kun aktivere om det ikke allerede finnes en periode i state store?
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
