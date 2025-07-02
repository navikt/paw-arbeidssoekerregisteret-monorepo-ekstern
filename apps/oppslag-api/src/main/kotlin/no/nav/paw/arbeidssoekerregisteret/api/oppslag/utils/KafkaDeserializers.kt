package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse

class PeriodeDeserializer : SpecificAvroDeserializer<Periode>()

class OpplysningerOmArbeidssoekerDeserializer : SpecificAvroDeserializer<OpplysningerOmArbeidssoeker>()

class ProfileringDeserializer : SpecificAvroDeserializer<Profilering>()

class EgenvurderingDeserializer : SpecificAvroDeserializer<Egenvurdering>()

class BekreftelseDeserializer : SpecificAvroDeserializer<Bekreftelse>()
