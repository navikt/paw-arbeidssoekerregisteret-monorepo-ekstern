package no.nav.paw.ledigestillinger.serde

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.pam.stilling.ext.avro.Ad

class AdAvroSerializer : SpecificAvroSerializer<Ad>()