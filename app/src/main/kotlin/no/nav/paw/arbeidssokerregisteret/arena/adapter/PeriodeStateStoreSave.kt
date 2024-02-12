package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.arena.v3.PeriodeListe
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration

fun KStream<Long, Periode>.saveToStore(storeName: String) {
    val processBuilder = { PeriodeStateStoreSave(storeName) }
    process(processBuilder, Named.`as`("periodeStateStoreSave"), storeName)
}


class PeriodeStateStoreSave(
    private val stateStoreName: String
) : Processor<Long, Periode, Long, PeriodeListe> {
    private var keyValueStore: KeyValueStore<Long, PeriodeListe>? = null
    private var context: ProcessorContext<Long, PeriodeListe>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun init(context: ProcessorContext<Long, PeriodeListe>?) {
        super.init(context)
        this.context = context
        keyValueStore = context?.getStateStore(stateStoreName)
        requireNotNull(context) { "Context er ikke satt" }
            .schedule(Duration.ofMinutes(10L), PunctuationType.STREAM_TIME) { streamTime ->
                val stateStore = context.getStateStore<KeyValueStore<Long, PeriodeListe>>(stateStoreName)
                stateStore.all().use { iterator ->
                    iterator.forEach { kv ->
                        val key = kv.key
                        val periodeListe = kv.value.removeClosedBefore(streamTime)
                        if (periodeListe != null) {
                            stateStore.put(key, periodeListe)
                        } else {
                            stateStore.delete(key)
                        }
                    }
                }
            }
    }

    fun PeriodeListe.removeClosedBefore(time: Long): PeriodeListe? {
        val periodsToKeep = perioder
            .toList()
            .filter { it.avsluttet == null || it.avsluttet.tidspunkt.plus(Duration.ofMinutes(5)).toEpochMilli() > time }
        return if (periodsToKeep.isEmpty()) {
            null
        } else {
            PeriodeListe(periodsToKeep)
        }
    }

    override fun process(record: Record<Long, Periode>?) {
        if (record == null) return
        process(
            requireNotNull(keyValueStore) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        db: KeyValueStore<Long, PeriodeListe>,
        record: Record<Long, Periode>
    ) {
        logger.debug("Lagrer periode i state store: key={}, id={}", record.key(), record.value().id)
        val list = (db.get(record.key()) ?: PeriodeListe(mutableListOf())).perioder
        val arenaPeriode = no.nav.paw.arbeidssokerregisteret.arena.v1.Periode(
            record.value().id,
            record.value().identitetsnummer,
            record.value().startet.toArena(),
            record.value().avsluttet?.toArena()
        )
        list.addLast(arenaPeriode)
        if (list.size > 2) list.drop(1)
        db.put(record.key(), PeriodeListe(list))
    }

    override fun close() {
        super.close()
        context = null
        keyValueStore = null
    }
}