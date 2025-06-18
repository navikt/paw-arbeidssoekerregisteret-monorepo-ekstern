package no.nav.paw.arbeidssoekerregisteret.topology.streams

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.kafka.processor.genericProcess
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.processor.api.Record

private val logger = buildApplicationLogger

fun StreamsBuilder.buildProfileringStream(
    applicationConfig: ApplicationConfig,
    profileringSerde: Serde<Profilering>,
) {
    logger.info("Oppretter KStream for periode")
    val kafkaTopology = applicationConfig.kafkaTopology
    this.stream(
        kafkaTopology.profileringTopic, Consumed.with(Serdes.Long(), profileringSerde)
    ).peek { key, _ ->
        logger.debug("Mottok event på ${kafkaTopology.profileringTopic} med key $key")
    }.genericProcess<Long, Profilering, Long, Profilering>(
        name = "handtereToggleForPeriode",
        stateStoreNames = arrayOf(kafkaTopology.stateStoreName)
    ) { record ->
        processProfilering(applicationConfig, record)
    }
}

fun processProfilering(
    applicationConfig: ApplicationConfig,
    record: Record<Long, Profilering>
): Record<Long, Profilering> {
    logger.info("Behandler profilering med key ${record.key()} og value ${record.value()}")
    return record
}