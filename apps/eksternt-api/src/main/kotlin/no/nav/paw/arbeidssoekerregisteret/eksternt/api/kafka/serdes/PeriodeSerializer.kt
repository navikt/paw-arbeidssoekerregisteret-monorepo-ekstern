package no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.serdes

import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.common.serialization.Serializer

class PeriodeSerializer : Serializer<Periode> {
    private val internalSerializer = KafkaAvroSerializer()

    override fun serialize(
        topic: String,
        data: Periode
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
