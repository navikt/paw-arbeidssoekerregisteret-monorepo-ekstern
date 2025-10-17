package no.nav.paw.ledigestillinger.serde

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import no.nav.pam.stilling.ext.avro.Ad

class AdAvroDeserializer : SpecificAvroDeserializer<Ad>()