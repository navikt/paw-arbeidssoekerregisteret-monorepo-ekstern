package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.filter
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.toArena
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v8.ArenaArbeidssokerregisterTilstand
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapWithContext
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
val bekreftelseHoeyvannsmerke = Instant.parse("2025-06-25T10:00:00Z")

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    profileringSerde: Serde<Profilering>,
    bekreftelseSerde: Serde<Bekreftelse>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>,
    ventendePeriodeStateStoreName: String,
    bekreftelseStateStoreName: String,
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

    builder.stream(
        topics.bekreftelse,
        Consumed.with(Serdes.Long(), bekreftelseSerde)
    )
        .filter { _, bekreftelse -> bekreftelse.svar.sendtInnAv.tidspunkt > bekreftelseHoeyvannsmerke }
        .filter { _, bekreftelse -> !bekreftelse.svar.vilFortsetteSomArbeidssoeker }
        .genericProcess<Long, Bekreftelse, Unit, Unit>(
            name = "lagre_bekreftelse",
            bekreftelseStateStoreName,
            punctuation = bekreftelsePunctuation(bekreftelseStateStoreName)
        ) { record ->
            val store: KeyValueStore<UUID, Bekreftelse> = getStateStore(bekreftelseStateStoreName)
            val lagret: Bekreftelse? = store.get(record.value().periodeId)
            if (lagret == null) {
                store.put(record.value().periodeId, record.value())
            } else {
                val erNyere = lagret.svar.sendtInnAv.tidspunkt.isAfter(record.value().svar.sendtInnAv.tidspunkt)
                logger.warn("Eksisterende bekreftelse med 'nei' funnet, lagret=${lagret.svar.sendtInnAv.tidspunkt}, mottatt=${record.value().svar.sendtInnAv.tidspunkt}, erNyere=$erNyere")
                if (erNyere) {
                    store.put(record.value().periodeId, record.value())
                }
            }
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
    }.mapWithContext(
        "berik_med_bekreftelse",
        bekreftelseStateStoreName
    ) { value ->
        val avsluttet = value.periode.avsluttet
        val bekreftelse = if ( avsluttet != null) {
            val bekreftelseStore: KeyValueStore<UUID, Bekreftelse> = getStateStore(bekreftelseStateStoreName)
            val bekreftelse = bekreftelseStore.get(value.periode.id)
            if (bekreftelse != null && !avsluttet.aarsak.contains("Ønsket ikke lenger å være arbeidssøker")) {
                logger.warn("Bekreftelse med svar 'nei' funnet, men periode avsluttet med aarsak='${avsluttet.aarsak}'")
            }
            bekreftelse?.periodeId?.also { id -> bekreftelseStore.delete(id) }
            Span.current().addEvent("periode_avsluttet", Attributes.builder()
                .put("bekreftelse_lagt_ved", bekreftelse != null)
                .put("aarsag_avsluttet", avsluttet.aarsak)
                .build()
            )
            bekreftelse
        } else {
            null
        }
        ArenaArbeidssokerregisterTilstand(
            value.periode,
            value.profilering,
            null,
            bekreftelse?.toArena()
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
            null,
            null
        )
    }.to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde),
    )
    return builder.build()
}
