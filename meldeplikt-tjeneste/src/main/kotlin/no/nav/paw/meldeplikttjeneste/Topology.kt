package no.nav.paw.meldeplikttjeneste

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapKeyAndValue
import no.nav.paw.meldeplikttjeneste.tilstand.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

fun topology(
    periodeTopic: String,
    ansvarsTopic: String,
    meldepliktTopic: String,
    builder: StreamsBuilder
): Topology {
    builder
        .stream<Long, Periode>(periodeTopic)
        .mapKeyAndValue("periodeId", STATE_STORE) { key, value ->
            val tilstand = if (value.avsluttet != null) {
                tomPeriode(key = key, id = value.id)
            } else {
                null
            }
            value.id to tilstand
        }
        .genericProcess("opprettEllerSlettPeriode", STATE_STORE) { record ->
            opprettEllerSlettPeriode(record)
        }
    return builder.build()
}

private fun ProcessorContext<UUID, InternTilstand?>.opprettEllerSlettPeriode(
    record: Record<UUID, InternTilstand?>
) {
    val keyValue: KeyValueStore<UUID, InternTilstand> = getStateStore(STATE_STORE)
    val tilstand = record.value()
    val gjeldeneTilstand = keyValue.get(record.key())
    if (tilstand == null) {
        keyValue.delete(record.key())
        if (gjeldeneTilstand != null) {
            forward(record.withValue(current))
        }
    } else {
        keyValue.put(record.key(), record.value())
        forward(record)
    }
}