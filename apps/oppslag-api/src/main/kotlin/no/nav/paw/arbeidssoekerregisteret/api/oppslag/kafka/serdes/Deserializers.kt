package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer

abstract class GenericDeserializer<T : SpecificRecord> : Deserializer<T> {
    private val internalDeserializer = KafkaAvroDeserializer()

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(
        topic: String,
        data: ByteArray
    ): T = internalDeserializer.deserialize(topic, data) as T

    override fun configure(
        configs: MutableMap<String, *>?,
        isKey: Boolean
    ) {
        internalDeserializer.configure(configs, isKey)
    }

    override fun close() {
        internalDeserializer.close()
    }
}

class PeriodeDeserializer : GenericDeserializer<Periode>()

class OpplysningerOmArbeidssoekerDeserializer : GenericDeserializer<OpplysningerOmArbeidssoeker>()

class ProfileringDeserializer : GenericDeserializer<Profilering>()
