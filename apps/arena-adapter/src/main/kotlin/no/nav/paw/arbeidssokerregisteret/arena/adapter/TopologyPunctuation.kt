package no.nav.paw.arbeidssokerregisteret.arena.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.kafka.processor.Punctuation
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration
import java.time.Instant
import java.util.*

data class ForsinkelseMetadata(
    val recordKey: Long,
    val traceparent: String?,
    val timestamp: Long
)

private val forsinkelseMetadataobjectMapper = ObjectMapper().registerKotlinModule()
val forsinkelseSerde: Serde<ForsinkelseMetadata> = Serdes.serdeFrom(
    { _, data -> forsinkelseMetadataobjectMapper.writeValueAsBytes(data) },
    { _, data -> forsinkelseMetadataobjectMapper.readValue<ForsinkelseMetadata>(data) }
)

private val interval = Duration.ofSeconds(2)
private val forsinkelseMs = 5000L

fun forsinkelsePunctuation(
    topicsJoinStateStoreName: String,
    ventendePeriodeStateStoreName: String
): Punctuation<Long, TopicsJoin> = Punctuation(
    interval = interval,
    type = PunctuationType.WALL_CLOCK_TIME
) { wallclock, context ->
    val ventende: KeyValueStore<UUID, ForsinkelseMetadata> = context.getStateStore(ventendePeriodeStateStoreName)
    val topicsJoinStore: KeyValueStore<UUID, TopicsJoin> = context.getStateStore(topicsJoinStateStoreName)
    val startTid = Instant.now()
    var counter = 0
    ventende.all().use { iterator ->
        iterator.asSequence()
            .filter { (wallclock.toEpochMilli() - it.value.timestamp) >= forsinkelseMs }
            .map { it.value to topicsJoinStore.get(it.key) }
            .map { (metadata, topicsJoin) ->
                Record(
                    metadata.recordKey,
                    topicsJoin,
                    wallclock.toEpochMilli(),
                    RecordHeaders().let {
                        if (metadata.traceparent != null) {
                            it.add("traceparent", metadata.traceparent.toByteArray() ?: byteArrayOf())
                        } else {
                            it
                        }
                    }
                )
            }
            .onEach {
                ventende.delete(it.value().periode.id)
                counter++
            }
            .forEach(context::forward)
    }
    val tidBrukt = Duration.between(startTid, Instant.now())
    logger.info("Punctuation with $counter elements took ${tidBrukt.toMillis()} ms")
}