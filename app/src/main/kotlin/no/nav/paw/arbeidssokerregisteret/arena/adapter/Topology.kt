package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.info
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import java.time.Duration

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    opplysningerOmArbeidssoekerSerde: Serde<OpplysningerOmArbeidssoeker>,
    profileringSerde: Serde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>
): Topology {
    builder
         .stream(topics.arbeidssokerperioder, Consumed.with(Serdes.Long(), periodeSerde))
        .saveToStore(stateStoreName)


    val opplysningerOmArbeidssoekerStream: KStream<Long, OpplysningerOmArbeidssoeker> = builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    )

    val profileringStream: KStream<Long, Profilering> =
        builder.stream(topics.profilering, Consumed.with(Serdes.Long(), profileringSerde))

    val joinWindow = JoinWindows.ofTimeDifferenceWithNoGrace(
        Duration.ofMinutes(5)
    )

    opplysningerOmArbeidssoekerStream
        .join(
            profileringStream,
            { opplysninger, profilering -> opplysninger to profilering },
            joinWindow
        ).loadAndMap(stateStoreName)
        .peek { _, value ->
            logger.info("Sending ${value.info()} to arena")
        }
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    val topology: Topology = builder.build()
    return topology
}