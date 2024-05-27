package no.nav.paw.arbeidssoekerregisteret.topology

import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.AIA_BEHOVSVURDERING
import no.nav.paw.arbeidssoekerregisteret.model.AIA_MIN_SIDE
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggleRecord
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggleState
import no.nav.paw.arbeidssoekerregisteret.model.buildEnableToggleRecord
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore

fun PeriodeInfo.erAvsluttet(): Boolean = avsluttet != null

private fun buildPunctuation(config: AppConfig): Punctuation<Long, ToggleState> {
    return Punctuation(
        config.regler.periodeTogglePunctuatorSchedule,
        PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        val stateStore: KeyValueStore<Long, ToggleState> = context.getStateStore(config.kafkaTopology.toggleStoreName)
        val iterator = stateStore.all()
        while (iterator.hasNext()) {
            val (periode, _) = iterator.next().value
            if (periode.avsluttet == null) {
                stateStore.delete(periode.arbeidssoekerId)
            }
            if (timestamp.minus(config.regler.utsattDeaktiveringAvAiaMinSide).isAfter(periode.avsluttet)) {
                stateStore.delete(periode.arbeidssoekerId)
                context.forward(buildDisableToggleRecord(periode, AIA_MIN_SIDE))
            }
        }
    }
}

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildPeriodeTopology(kafkaKeyFunction: (String) -> KafkaKeysResponse) {
    val (kafkaTopology) = appConfig

    this.stream<Long, Periode>(kafkaTopology.periodeTopic)
        .mapValues { periode -> buildPeriodeInfo(periode, kafkaKeyFunction(periode.identitetsnummer).id) }
        .genericProcess<Long, PeriodeInfo, Long, ToggleState>(
            name = kafkaTopology.periodeToggleProcessor,
            stateStoreNames = arrayOf(kafkaTopology.toggleStoreName),
            punctuation = buildPunctuation(appConfig)
        ) { record ->
            val keyValueStore: KeyValueStore<Long, ToggleState> = getStateStore(kafkaTopology.toggleStoreName)
            val periode = record.value()
            when {
                periode.erAvsluttet() -> {
                    // Lagre perioden i state store for å vente med deaktivering AIA Min Side
                    keyValueStore.put(periode.arbeidssoekerId, buildDisableToggleState(periode, AIA_MIN_SIDE))

                    // Send event for å deaktivere AIA Behovsvurdering
                    forward(buildDisableToggleRecord(periode, AIA_BEHOVSVURDERING))
                }

                else -> {
                    // Send event for å aktivere AIA Min Side
                    forward(buildEnableToggleRecord(periode, AIA_MIN_SIDE))

                    // Send event for å aktivere AIA Behovsvurdering
                    forward(buildEnableToggleRecord(periode, AIA_BEHOVSVURDERING))
                }
            }
        }
        .mapValues { toggleState ->
            return@mapValues toggleState.toggle
        }
        .to(kafkaTopology.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}
