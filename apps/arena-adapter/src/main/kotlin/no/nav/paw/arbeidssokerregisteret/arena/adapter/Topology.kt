package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.filter
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.toArena
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v5.ArenaArbeidssokerregisterTilstand
import no.nav.paw.kafka.processor.genericProcess
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

val HOEYVANNSMERKE = Instant.parse("2024-01-01T00:00:00Z")

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    profileringSerde: Serde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>,
    ventendePeriodeStateStoreName: String
): Topology {
    val perioder = builder.stream(
        topics.arbeidssokerperioder,
        Consumed.with(Serdes.Long(), periodeSerde)
    ).filter { _, periode ->
        periode.startet.tidspunkt.isAfter(HOEYVANNSMERKE) ||
                (periode.avsluttet != null && periode.avsluttet.tidspunkt.isAfter(HOEYVANNSMERKE))
    }

    val profileringer = builder.stream(
        topics.profilering,
        Consumed.with(Serdes.Long(), profileringSerde)
    ).filter("filterOnRecordTimestamp") { record ->
        Instant.ofEpochMilli(record.timestamp()).isAfter(HOEYVANNSMERKE)
    }


    perioder.genericProcess<Long, Periode, Long, TopicsJoin>(
        name = "haandter_periode",
        stateStoreName,
        ventendePeriodeStateStoreName,
        punctuation = forsinkelsePunctuation(
            topicsJoinStateStoreName = stateStoreName,
            ventendePeriodeStateStoreName = ventendePeriodeStateStoreName
        )
    ) { record ->
        val periode = record.value()
        val store: KeyValueStore<UUID, TopicsJoin> = getStateStore(stateStoreName)
        val ventendePeriodeStore: KeyValueStore<UUID, ForsinkelseMetadata> =
            getStateStore(ventendePeriodeStateStoreName)
        if (periode.avsluttet != null) {
            store.delete(periode.id)
            ventendePeriodeStore.delete(periode.id)
            forward(record.withValue(TopicsJoin(toArena(periode), null, null)))
        } else {
            val topicsJoin = store.get(periode.id)
            if (topicsJoin == null) {
                store.put(periode.id, TopicsJoin(toArena(periode), null, null))
                ventendePeriodeStore.putIfAbsent(
                    periode.id, ForsinkelseMetadata(
                        recordKey = record.key(),
                        traceparent = record.headers().lastHeader("traceparent")?.let { String(it.value()) },
                        timestamp = currentSystemTimeMs()
                    )
                )
            } else {
                if (topicsJoin.periode == null) {
                    val oppdatertJoin = TopicsJoin(toArena(periode), topicsJoin.profilering, null)
                    store.put(periode.id, oppdatertJoin)
                    if (oppdatertJoin.profilering != null) {
                        forward(record.withValue(oppdatertJoin))
                    } else {
                        ventendePeriodeStore.putIfAbsent(
                            periode.id, ForsinkelseMetadata(
                                recordKey = record.key(),
                                traceparent = record.headers().lastHeader("traceparent")?.let { String(it.value()) },
                                timestamp = currentSystemTimeMs()
                            )
                        )
                    }
                }
            }
        }
    }.mapValues { _, value ->
        ArenaArbeidssokerregisterTilstand(
            value.periode,
            value.profilering,
            null
        )
    }.to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde)
    )

    profileringer.genericProcess<Long, Profilering, Long, TopicsJoin>(
        name = "haandter_profilering",
        stateStoreName,
        ventendePeriodeStateStoreName,
        punctuation = null
    ) { record ->
        val profilering = record.value()
        val store: KeyValueStore<UUID, TopicsJoin> = getStateStore(stateStoreName)
        val ventendePeriodeStore: KeyValueStore<UUID, ForsinkelseMetadata> =
            getStateStore(ventendePeriodeStateStoreName)
        val existingValue = store.get(profilering.periodeId)
        if (existingValue?.profilering == null) {
            val oppdatertJoin = TopicsJoin(existingValue?.periode, toArena(profilering), null)
            store.put(profilering.periodeId, oppdatertJoin)
            if (oppdatertJoin.periode != null) {
                ventendePeriodeStore.delete(profilering.periodeId)
                forward(record.withValue(oppdatertJoin))
            }
        }
    }.mapValues { _, value ->
        ArenaArbeidssokerregisterTilstand(
            value.periode,
            value.profilering,
            null
        )
    }.to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde),
    )
    return builder.build()
}
