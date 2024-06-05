package no.nav.paw.arbeidssoekerregisteret.topology

import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakInfoSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.config.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtakInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.buildDisableToggle
import no.nav.paw.arbeidssoekerregisteret.model.buildRecord
import no.nav.paw.arbeidssoekerregisteret.model.erAvsluttet
import no.nav.paw.arbeidssoekerregisteret.model.erInnenfor
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapKeyAndValue
import no.nav.paw.config.kafka.streams.mapNonNull
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.state.KeyValueStore

context(ConfigContext, LoggingContext)
fun StreamsBuilder.buildSiste14aVedtakTopology(
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?,
    hentFolkeregisterIdent: (aktorId: String) -> IdentInformasjon?
) {
    val kafkaStreamsConfig = appConfig.kafkaStreams
    val microfrontendConfig = appConfig.microfrontends

    this.stream(
        kafkaStreamsConfig.siste14aVedtakTopic, Consumed.with(Serdes.String(), buildSiste14aVedtakSerde())
    ).mapNonNull("mapKeyTilPdlIdent") { siste14aVedtak ->
        hentFolkeregisterIdent(siste14aVedtak.aktorId.get())
            ?.let { identInfo -> identInfo to siste14aVedtak }
    }.mapKeyAndValue("mapKeyTilKafkaKeys") { _, (identInfo, siste14aVedtak) ->
        hentKafkaKeys(identInfo.ident)?.let { kafkaKeysResponse ->
            kafkaKeysResponse.key to Siste14aVedtakInfo(
                siste14aVedtak.aktorId.get(), identInfo.ident, kafkaKeysResponse.id, siste14aVedtak.fattetDato
            )
        }
    }.repartition(
        Repartitioned.numberOfPartitions<Long?, Siste14aVedtakInfo?>(6) // TODO Må stemme med partisjonering for periodetopics?
            .withKeySerde(Serdes.Long()).withValueSerde(buildSiste14aVedtakInfoSerde())
    ).genericProcess<Long, Siste14aVedtakInfo, Long, Toggle>(
        name = "handtereToggleFor14aVedtak", stateStoreNames = arrayOf(kafkaStreamsConfig.periodeStoreName)
    ) { record ->
        val siste14aVedtakInfo = record.value()
        val keyValueStore: KeyValueStore<Long, PeriodeInfo> = getStateStore(kafkaStreamsConfig.periodeStoreName)
        val periodeInfo = keyValueStore.get(siste14aVedtakInfo.arbeidssoekerId)

        // Sjekk om vedtak er innenfor en aktiv periode,
        if (periodeInfo != null && !periodeInfo.erAvsluttet() && periodeInfo.erInnenfor(siste14aVedtakInfo.fattetDato)) {
            logger.debug(
                "Det ble gjort et 14a vedtak for arbeidsøkerperiode {}. Iverksetter deaktivering av {}.",
                periodeInfo.id,
                microfrontendConfig.aiaBehovsvurdering
            )
            val toggle = periodeInfo.buildDisableToggle(microfrontendConfig.aiaBehovsvurdering)
            forward(toggle.buildRecord(siste14aVedtakInfo.arbeidssoekerId))
        }
    }.to(kafkaStreamsConfig.microfrontendTopic, Produced.with(Serdes.Long(), buildToggleSerde()))
}