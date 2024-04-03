package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.arena.v4.ArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import kotlin.reflect.KClass

fun KStream<Long, out SpecificRecord>.saveToStoreForwardIfComplete(
    type: KClass<out BaseStateStoreSave>,
    storeName: String,
    registry: PrometheusMeterRegistry
): KStream<Long, ArenaArbeidssokerregisterTilstand> {
    val processBuilder = {
        when (type) {
            ProfileringStateStoreSave::class -> ProfileringStateStoreSave(storeName, registry)
            OpplysningerOmArbeidssoekerStateStoreSave::class -> OpplysningerOmArbeidssoekerStateStoreSave(storeName, registry)
            PeriodeStateStoreSave::class -> PeriodeStateStoreSave(storeName, registry)
            else -> throw IllegalArgumentException("Ukjent type ${type.simpleName}")
        }
    }
    return process(processBuilder, Named.`as`(type.simpleName), storeName)
}

