package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.processor.StreamPartitioner
import java.util.*

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
    ).selectKey { key, value -> compoundKey(key, value.id) }
        .repartition(
            Repartitioned.with(Serdes.String(), periodeSerde)
                .withStreamPartitioner(arenaStreamPartitioner())
        )
        .saveToStoreForwardIfComplete(
            type = PeriodeStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    ).selectKey { key, value -> compoundKey(key, value.periodeId) }
        .repartition(
            Repartitioned.with(Serdes.String(), opplysningerOmArbeidssoekerSerde)
                .withStreamPartitioner(arenaStreamPartitioner())
        )
        .saveToStoreForwardIfComplete(
            type = OpplysningerOmArbeidssoekerStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(topics.arena, Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde))

    builder.stream(
        topics.profilering,
        Consumed.with(Serdes.Long(), profileringSerde)
    ).selectKey { key, value -> compoundKey(key, value.periodeId) }
        .repartition(
            Repartitioned.with(Serdes.String(), profileringSerde)
                .withStreamPartitioner(arenaStreamPartitioner())
        )
        .saveToStoreForwardIfComplete(
            type = ProfileringStateStoreSave::class,
            storeName = stateStoreName
        )
        .to(
            topics.arena,
            Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde),
        )

    val topology: Topology = builder.build()
    return topology
}

fun compoundKey(key: Long, id: UUID): String {
    return "$key:$id"
}

fun originalKeyFromCompoundKey(compoundKey: String): Long {
    return compoundKey.split(":").first().toLong()
}

fun <A> arenaStreamPartitioner(): StreamPartitioner<String, A> {
    return ArenaStreamPartitioner()
}

class ArenaStreamPartitioner<A> : StreamPartitioner<String, A> {
    @Deprecated(
        message = "Deprecated in Java",
        replaceWith = ReplaceWith("partitions(topic, key, value, numPartitions)")
    )
    override fun partition(topic: String?, key: String?, value: A?, numPartitions: Int): Int? {
        return key?.let { originalKeyFromCompoundKey(it) }?.hashCode()?.mod(numPartitions)
    }
}
