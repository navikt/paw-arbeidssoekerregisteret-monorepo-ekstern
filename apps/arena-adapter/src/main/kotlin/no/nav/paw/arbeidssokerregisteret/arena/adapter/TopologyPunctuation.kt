package no.nav.paw.arbeidssokerregisteret.arena.adapter

import tools.jackson.databind.MapperFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.processor.Punctuation
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.*
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy

data class ForsinkelseMetadata(
    val recordKey: Long,
    val traceparent: String?,
    val timestamp: Long
)

private val forsinkelseMetadataobjectMapper = jacksonMapperBuilder {
    disable(KotlinFeature.SingletonSupport)
    disable(KotlinFeature.StrictNullChecks)
}
    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    .disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
    .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
    .build()
val forsinkelseSerde: Serde<ForsinkelseMetadata> = Serdes.serdeFrom(
    { _, data -> forsinkelseMetadataobjectMapper.writeValueAsBytes(data) },
    { _, data -> forsinkelseMetadataobjectMapper.readValue<ForsinkelseMetadata>(data) }
)

private val periodeForsinkelseInterval = Duration.ofSeconds(2)
private val periodeForsinkelseMs = 5000L

fun forsinkelsePunctuation(
    topicsJoinStateStoreName: String,
    ventendePeriodeStateStoreName: String
): Punctuation<Long, TopicsJoin> = Punctuation(
    interval = periodeForsinkelseInterval,
    type = PunctuationType.WALL_CLOCK_TIME
) { wallclock, context ->
    val ventende: KeyValueStore<UUID, ForsinkelseMetadata> = context.getStateStore(ventendePeriodeStateStoreName)
    val topicsJoinStore: KeyValueStore<UUID, TopicsJoin> = context.getStateStore(topicsJoinStateStoreName)
    val startTid = Instant.now()
    var counter = 0
    ventende.all().use { iterator ->
        iterator.asSequence()
            .filter { (wallclock.toEpochMilli() - it.value.timestamp) >= periodeForsinkelseMs }
            .map { it.value to topicsJoinStore.get(it.key) }
            .filter { (metadata, topicsJoin) ->
                (topicsJoin.periode != null).also { harPeriode ->
                    if (!harPeriode) {
                        logger.warn(
                            "TopicJoin: periode={}, profilering={}, metadata=({}), partition={}",
                            topicsJoin.periode != null,
                            topicsJoin.profilering != null,
                            metadata,
                            context.taskId().partition()
                        )
                    }
                }
            }
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

val bekreftelseRyddigIntervall = Duration.ofSeconds(120)
val bekreftelseForsinkelseFoerRydding = Duration.ofHours(24)


fun bekreftelsePunctuation(
    bekreftelseStoreName: String
): Punctuation<Unit, Unit> = Punctuation(
    interval = bekreftelseRyddigIntervall,
    type = PunctuationType.WALL_CLOCK_TIME
) { wallclock, context ->
    val bekreftelser: KeyValueStore<UUID, Bekreftelse> = context.getStateStore(bekreftelseStoreName)
    val startTid = Instant.now()
    var totalt = 0
    val antallSlettet = bekreftelser.all().use { iterator ->
        iterator.asSequence()
            .onEach { totalt++ }
            .filter {
                logger.trace(
                    "Sjekker bekreftelse sendtInn={} mot wallclock={}",
                    it.value.svar.sendtInnAv.tidspunkt,
                    wallclock
                )
                between(it.value.svar.sendtInnAv.tidspunkt, wallclock) >= bekreftelseForsinkelseFoerRydding
            }
            .onEach {
                bekreftelser.delete(it.key)
            }.count()
    }
    val tidBrukt = between(startTid, Instant.now())
    logger.info("Slettet $antallSlettet av $totalt bekreftelser, tid brukt: ${tidBrukt.toMillis()} ms")
}
