package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

fun KStream<Long, Periode>.saveToStore(storeName: String) {
    val processBuilder = { PeriodeStateStoreSave(storeName) }
    process(processBuilder, Named.`as`("periodeStateStoreSave"), storeName)
}


class PeriodeStateStoreSave(
    private val stateStoreName: String
) : Processor<Long, Periode, Long, Periode> {
    private var keyValueStore: KeyValueStore<Long, Periode>? = null
    private var context: ProcessorContext<Long, Periode>? = null

    override fun init(context: ProcessorContext<Long, Periode>?) {
        super.init(context)
        this.context = context
        keyValueStore = context?.getStateStore(stateStoreName)
    }

    override fun process(record: Record<Long, Periode>?) {
        if (record == null) return
        process(
            requireNotNull(keyValueStore) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        db: KeyValueStore<Long, Periode>,
        record: Record<Long, Periode>
    ) {
        val periode = record.value().takeIf { it.avsluttet == null }
        if (periode != null) {
            db.put(record.key(), periode)
        } else {
            db.delete(record.key())
        }
    }

    override fun close() {
        super.close()
        context = null
        keyValueStore = null
    }
}