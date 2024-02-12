package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import kotlinx.coroutines.flow.merge
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.arena.v3.TempArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    opplysningerOmArbeidssoekerSerde: Serde<OpplysningerOmArbeidssoeker>,
    profileringSerde: Serde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>
): Topology {
    builder.stream(
        topics.arbeidssokerperioder,
        Consumed.with(Serdes.Long(), periodeSerde)
    ).selectKey { key, value -> "$key:${value.id}" }
        .saveToStoreForwardIfComplete(
            type = PeriodeStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    ).selectKey { key, value -> "$key:${value.periodeId}" }
        .saveToStoreForwardIfComplete(
            type = OpplysningerOmArbeidssoekerStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    builder.stream(
        topics.profilering,
        Consumed.with(Serdes.Long(), profileringSerde)
    ).selectKey { key, value -> "$key:${value.periodeId}" }
        .saveToStoreForwardIfComplete(
            type = ProfileringStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    val topology: Topology = builder.build()
    return topology
}
