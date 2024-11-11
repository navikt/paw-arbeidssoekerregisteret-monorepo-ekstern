package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse

class PeriodeSerializer : SpecificAvroSerializer<Periode>()

class OpplysningerOmArbeidssoekerSerializer : SpecificAvroSerializer<OpplysningerOmArbeidssoeker>()

class ProfileringSerializer : SpecificAvroSerializer<Profilering>()

class BekreftelseSerializer : SpecificAvroSerializer<Bekreftelse>()
