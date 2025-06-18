package no.nav.paw.arbeidssoekerregisteret.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering

class EgenvurderingSerializer : SpecificAvroSerializer<Egenvurdering>()
class EgenvurderingDeserializer : SpecificAvroDeserializer<Egenvurdering>()