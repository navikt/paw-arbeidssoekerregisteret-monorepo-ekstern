package no.nav.paw.arbeidssoekerregisteret.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggleRecord
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggleState
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggleRecord
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.Logger

fun PeriodeInfo.erAvsluttet(): Boolean = avsluttet != null

private fun buildPunctuation(config: AppConfig, logger: Logger): Punctuation<Long, ToggleState> {
    return Punctuation(
        config.regler.periodeTogglePunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        val stateStore: KeyValueStore<Long, ToggleState> = context.getStateStore(config.kafkaTopology.toggleStoreName)
        val iterator = stateStore.all()
        while (iterator.hasNext()) {
            val (periode, _) = iterator.next().value
            // Aktive perioder skal ikke ligge i state store, så sletter den
            if (periode.avsluttet == null) {
                stateStore.delete(periode.arbeidssoekerId)
            }
            // Om det er gått mer en utsettelsestid (21 dager) fra perioden ble avsluttet så send event for å
            // deaktivere AIA Min Side
            if (timestamp.minus(config.regler.utsattDeaktiveringAvAiaMinSide).isAfter(periode.avsluttet)) {
                logger.debug(
                    "Utsettelsestid for arbeidsøkerperiode {} er utløpt. Iverksetter forsinket deaktivering av {}.",
                    periode.id,
                    config.microfrontends.aiaMinSide
                )
                stateStore.delete(periode.arbeidssoekerId)
                context.forward(buildDisableToggleRecord(periode, config.microfrontends.aiaMinSide))
            }
        }
    }
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildPeriodeTopology(kafkaKeysClient: KafkaKeysClient) {
    val (kafkaTopology, _, microfrontends) = appConfig

    this.stream<Long, Periode>(kafkaTopology.periodeTopic)
        .mapValues { periode ->
            val response = getKafkaKey(periode.identitetsnummer, kafkaKeysClient)
            val arbeidssoekerId = checkNotNull(response?.id) { "KafkaKeysResponse er null" }
            buildPeriodeInfo(periode, arbeidssoekerId)
        }.genericProcess<Long, PeriodeInfo, Long, ToggleState>(
            name = kafkaTopology.periodeToggleProcessor,
            stateStoreNames = arrayOf(kafkaTopology.toggleStoreName),
            punctuation = buildPunctuation(appConfig, logger)
        ) { record ->
            val keyValueStore: KeyValueStore<Long, ToggleState> = getStateStore(kafkaTopology.toggleStoreName)
            val periode = record.value()
            when {
                periode.erAvsluttet() -> {
                    logger.debug(
                        "Arbeidsøkerperiode {} ble avluttet. Iverksetter deaktivering av {}. Lagrer forsinket deaktivering av {}.",
                        periode.id,
                        microfrontends.aiaBehovsvurdering,
                        microfrontends.aiaMinSide
                    )
                    // Lagre perioden i state store for å vente med deaktivering AIA Min Side
                    keyValueStore.put(
                        periode.arbeidssoekerId, buildDisableToggleState(periode, microfrontends.aiaMinSide)
                    )

                    // Send event for å deaktivere AIA Behovsvurdering
                    forward(buildDisableToggleRecord(periode, microfrontends.aiaBehovsvurdering))
                }

                else -> {
                    logger.debug(
                        "Arbeidsøkerperiode {} er aktiv. Iverksetter aktivering av {} og {}.",
                        periode.id,
                        microfrontends.aiaMinSide,
                        microfrontends.aiaBehovsvurdering
                    )
                    // Send event for å aktivere AIA Min Side
                    forward(buildEnableToggleRecord(periode, microfrontends.aiaMinSide))

                    // Send event for å aktivere AIA Behovsvurdering
                    forward(buildEnableToggleRecord(periode, microfrontends.aiaBehovsvurdering))
                }
            }
        }.mapValues { toggleState ->
            return@mapValues toggleState.toggle
        }.to(kafkaTopology.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}

fun getKafkaKey(identitetsnummer: String, kafkaKeysClient: KafkaKeysClient): KafkaKeysResponse? = runBlocking {
    kafkaKeysClient.getIdAndKey(identitetsnummer)
}
