package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.arena.v8.ArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import kotlin.reflect.KClass

fun KStream<Long, out SpecificRecord>.saveToStoreForwardIfComplete(
    type: KClass<out BaseStateStoreSave>,
    storeName: String,
    registry: PrometheusMeterRegistry
): KStream<Long, ArenaArbeidssokerregisterTilstand> {
    val processorSupplier = StateStoreSaveSupplier(type, storeName, registry)
    return process(processorSupplier, Named.`as`(type.simpleName), storeName)
}

