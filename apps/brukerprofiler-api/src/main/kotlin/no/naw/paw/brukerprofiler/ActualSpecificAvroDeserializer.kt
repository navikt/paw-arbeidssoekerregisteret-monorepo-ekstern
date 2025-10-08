package no.naw.paw.brukerprofiler

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import org.apache.avro.specific.SpecificRecord

class ActualSpecificAvroDeserializer : SpecificAvroDeserializer<SpecificRecord>()