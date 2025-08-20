package no.nav.paw

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.opentelemetry.api.trace.Span
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.consumer.toRow
import no.nav.paw.oppslagsapi.periode
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serde
import java.time.Duration
import java.time.Instant

object TestData {
    private val metdataFactory = MetadataFactory.create()
    val periode_a_startet = periode(identitetsnummer = Identitetsnummer("12345678901"), startet = Instant.now())
    val periode_a_opplysninger = createOpplysninger(
        periodeId = periode_a_startet.id,
        sendtInnAv = metdataFactory.build(tidspunkt = Instant.now())
    )
    val periode_a_paa_vegne_av_startet = startPaaVegneAv(periodeId = periode_a_startet.id)
    val periode_a_paa_vegne_av_stoppet = stoppPaaVegneAv(periodeId = periode_a_startet.id)
    val periode_a_bekreftelse = bekreftelseMelding(periodeId = periode_a_startet.id)
    val periode_a_avsluttet = periode(
        periodeId = periode_a_startet.id,
        identitetsnummer = Identitetsnummer(periode_a_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_a_startet.startet.tidspunkt
    )
    val periode_b_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_b_opplysninger = createOpplysninger(
        periodeId = periode_b_startet.id,
        sendtInnAv = metdataFactory.build(tidspunkt = Instant.now())
    )
    val periode_b_avsluttet = periode(
        periodeId = periode_b_startet.id,
        identitetsnummer = Identitetsnummer(periode_b_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_b_startet.startet.tidspunkt
    )
    val periode_c_startet = periode(identitetsnummer = Identitetsnummer("12345678902"), startet = Instant.now())
    val periode_c_opplysninger = createOpplysninger(
        periodeId = periode_c_startet.id,
        sendtInnAv = metdataFactory.build(tidspunkt = Instant.now())
    )
    val periode_c_profilering = createProfilering(
        periodeId = periode_c_startet.id,
        opplysningerId = periode_c_opplysninger.id,
        sendtInnAv = metdataFactory.build(tidspunkt = Instant.now())
    )
    val periode_c_avsluttet = periode(
        periodeId = periode_c_startet.id,
        identitetsnummer = Identitetsnummer(periode_c_startet.identitetsnummer),
        avsluttet = Instant.now() + Duration.ofMinutes(1),
        startet = periode_c_startet.startet.tidspunkt
    )

    val data = listOf(
        periode_a_startet,
        periode_a_opplysninger,
        periode_a_paa_vegne_av_startet,
        periode_a_paa_vegne_av_stoppet,
        periode_a_bekreftelse,
        periode_a_avsluttet,
        periode_b_startet,
        periode_b_opplysninger,
        periode_b_avsluttet,
        periode_c_startet,
        periode_c_opplysninger,
        periode_c_profilering,
        periode_c_avsluttet
    )

    val dataRows get() =
        data
            .map(SpecificRecord::toConsumerRecord)
            .map{ it.toRow(serde.deserializer()) to Span.current()}
}

fun SpecificRecord.toConsumerRecord(): ConsumerRecord<Long, ByteArray> {
    return ConsumerRecord(
        "topic",
        0,
        0L,
        0L,
        serde.serializer().serialize("topic", this)
    )
}

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"

fun <T : SpecificRecord> opprettSerde(): Serde<T> {
    val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
    val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
    serde.configure(
        mapOf(
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
        ),
        false
    )
    return serde
}