package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.serdes

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.common.serialization.Deserializer

class PeriodeDeserializer : Deserializer<Periode> {
    private val internalDeserializer = KafkaAvroDeserializer()

    override fun deserialize(
        topic: String,
        data: ByteArray
    ): Periode = internalDeserializer.deserialize(topic, data) as Periode

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
