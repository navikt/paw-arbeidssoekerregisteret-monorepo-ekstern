package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serializer

abstract class GenericSerializer<T : SpecificRecord> : Serializer<T> {
    private val internalSerializer = KafkaAvroSerializer()

    override fun serialize(
        topic: String,
        data: T
    ): ByteArray = internalSerializer.serialize(topic, data) as ByteArray

    override fun configure(
        configs: MutableMap<String, *>?,
        isKey: Boolean
    ) {
        internalSerializer.configure(configs, isKey)
    }

    override fun close() {
        internalSerializer.close()
    }
}

class PeriodeSerializer : GenericSerializer<Periode>()

class OpplysningerOmArbeidssoekerSerializer : GenericSerializer<OpplysningerOmArbeidssoeker>()

class ProfileringSerializer : GenericSerializer<Profilering>()
