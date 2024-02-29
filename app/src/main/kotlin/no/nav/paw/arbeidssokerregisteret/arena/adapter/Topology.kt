package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.OpplysningerOmArbeidssoekerStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.PeriodeStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.ProfileringStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.saveToStoreForwardIfComplete
import no.nav.paw.arbeidssokerregisteret.arena.v4.ArenaArbeidssokerregisterTilstand
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    opplysningerOmArbeidssoekerSerde: Serde<OpplysningerOmArbeidssoeker>,
    profileringSerde: Serde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>,
    registry: PrometheusMeterRegistry
): Topology {
    builder.stream(
        topics.arbeidssokerperioder,
        Consumed.with(Serdes.Long(), periodeSerde)
    ).saveToStoreForwardIfComplete(
        type = PeriodeStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    ).to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde)
    )

    builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    ).saveToStoreForwardIfComplete(
        type = OpplysningerOmArbeidssoekerStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    ).to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde)
    )

    builder.stream(
        topics.profilering,
        Consumed.with(Serdes.Long(), profileringSerde)
    ).saveToStoreForwardIfComplete(
        type = ProfileringStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    ).to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde),
    )

    val topology: Topology = builder.build()
    return topology
}
