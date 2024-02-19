package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import kotlin.reflect.KClass

fun KStream<Long, out SpecificRecord>.saveToStoreForwardIfComplete(type: KClass<out BaseStateStoreSave>, storeName: String): KStream<Long, ArenaArbeidssokerregisterTilstand> {
    val processBuilder = {
        when (type) {
            ProfileringStateStoreSave::class -> ProfileringStateStoreSave(storeName)
            OpplysningerOmArbeidssoekerStateStoreSave::class -> OpplysningerOmArbeidssoekerStateStoreSave(storeName)
            PeriodeStateStoreSave::class -> PeriodeStateStoreSave(storeName)
            else -> throw IllegalArgumentException("Ukjent type ${type.simpleName}")
        }
    }
    return process(processBuilder, Named.`as`(type.simpleName), storeName)
}

