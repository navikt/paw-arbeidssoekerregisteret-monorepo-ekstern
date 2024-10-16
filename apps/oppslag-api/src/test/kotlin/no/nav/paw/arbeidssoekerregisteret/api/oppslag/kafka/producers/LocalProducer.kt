package no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.producers

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.OpplysningerOmArbeidssoekerSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.PeriodeSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.ProfileringSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer
import java.time.Instant
import java.util.*

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

    produserMeldinger(kafkaConfig, applicationConfig, TestMessages()::perioder, applicationConfig.periodeTopic, PeriodeSerializer())
    produserMeldinger(kafkaConfig, applicationConfig, TestMessages()::opplysningerOmArbeidssoeker, applicationConfig.opplysningerOmArbeidssoekerTopic, OpplysningerOmArbeidssoekerSerializer())
    produserMeldinger(kafkaConfig, applicationConfig, TestMessages()::profilering, applicationConfig.profileringTopic, ProfileringSerializer())
}

fun <T : SpecificRecord> produserMeldinger(
    kafkaConfig: KafkaConfig,
    applicationConfig: ApplicationConfig,
    messages: () -> List<T>,
    topic: String,
    serializer: Serializer<T>
) {
    val localProducer = LocalProducer(kafkaConfig, applicationConfig, serializer)
    try {
        messages().forEach { message ->
            localProducer.produceMessage(topic, 1234L, message)
        }
    } catch (e: Exception) {
        println("LocalProducer $topic error: ${e.message}")
    } finally {
        localProducer.closeProducer()
    }
}

class LocalProducer<T : SpecificRecord>(
    private val kafkaConfig: KafkaConfig,
    private val applicationConfig: ApplicationConfig,
    private val valueSerializer: Serializer<T>
) {
    private lateinit var producer: Producer<Long, T>

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer =
            kafkaFactory.createProducer<Long, T>(
                clientId = applicationConfig.gruppeId,
                keySerializer = LongSerializer::class,
                valueSerializer = valueSerializer::class
            )
    }

    fun produceMessage(
        topic: String,
        key: Long,
        value: T
    ) {
        val record = ProducerRecord(topic, key, value)
        producer.send(record) { _, exception ->
            if (exception != null) {
                println("Failed to send message to topic $topic: $exception")
            } else {
                println("Message sent successfully to topic: $topic")
            }
        }
    }

    fun closeProducer() {
        producer.close()
    }
}

class TestMessages {
    val testPeriodeId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val testPeriodeId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val testOpplysningerId1 = UUID.fromString("00000000-0000-0000-0000-000000000003")

    fun perioder(): List<Periode> =
        listOf(
            Periode(
                testPeriodeId1,
                "12345678901",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678901"
                    ),
                    "kilde1a",
                    "aarsak1a",
                    TidspunktFraKilde(
                        Instant.now(),
                        AvviksType.UKJENT_VERDI
                    )
                ),
                null
            ),
            Periode(
                testPeriodeId2,
                "12345678902",
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678902"
                    ),
                    "kilde2a",
                    "aarsak2a",
                    TidspunktFraKilde(
                        Instant.now(),
                        AvviksType.UKJENT_VERDI
                    )
                ),
                Metadata(
                    Instant.now().plusSeconds(100),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678902"
                    ),
                    "kilde2b",
                    "aarsak2b",
                    TidspunktFraKilde(
                        Instant.now(),
                        AvviksType.UKJENT_VERDI
                    )
                )
            )
        )

    fun opplysningerOmArbeidssoeker(): List<OpplysningerOmArbeidssoeker> =
        listOf(
            OpplysningerOmArbeidssoeker(
                testOpplysningerId1,
                testPeriodeId1,
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678901"
                    ),
                    "kilde3a",
                    "aarsak3a",
                    TidspunktFraKilde(
                        Instant.now(),
                        AvviksType.UKJENT_VERDI
                    )
                ),
                Utdanning(
                    "NUS_KODE",
                    JaNeiVetIkke.JA,
                    JaNeiVetIkke.JA
                ),
                Helse(JaNeiVetIkke.JA),
                Jobbsituasjon(
                    listOf(
                        BeskrivelseMedDetaljer(
                            Beskrivelse.AKKURAT_FULLFORT_UTDANNING,
                            mapOf(
                                Pair("noekkel1a", "verdi1a"),
                                Pair("noekkel1b", "verdi1b")
                            )
                        ),
                        BeskrivelseMedDetaljer(
                            Beskrivelse.DELTIDSJOBB_VIL_MER,
                            mapOf(
                                Pair("noekkel2a", "verdi2a"),
                                Pair("noekkel2b", "verdi2b")
                            )
                        )
                    )
                ),
                Annet(JaNeiVetIkke.JA)
            )
        )

    fun profilering(): List<Profilering> =
        listOf(
            Profilering(
                UUID.randomUUID(),
                testPeriodeId1,
                testOpplysningerId1,
                Metadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.UKJENT_VERDI,
                        "12345678901"
                    ),
                    "kilde4a",
                    "aarsak4a",
                    TidspunktFraKilde(
                        Instant.now(),
                        AvviksType.UKJENT_VERDI
                    )
                ),
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                true,
                30
            )
        )
}
