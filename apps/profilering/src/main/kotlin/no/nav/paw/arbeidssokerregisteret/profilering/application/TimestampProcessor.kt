package no.nav.paw.arbeidssokerregisteret.profilering.application

import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

class TimestampProcessor<K, V> : Processor<K, V, K, V> {
    private lateinit var context: ProcessorContext<K, V>

    override fun init(context: ProcessorContext<K, V>) {
        this.context = context
    }

    override fun process(record: Record<K, V>) {
        context.forward(record.withTimestamp(Instant.now().toEpochMilli()))
    }
}
