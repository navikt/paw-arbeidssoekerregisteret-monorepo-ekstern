package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

class PeriodeSerializer : SpecificAvroSerializer<Periode>()
class PeriodeDeserializer : SpecificAvroDeserializer<Periode>()
