package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.BekreftelseSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.OpplysningerOmArbeidssoekerSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PeriodeSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ProfileringSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

fun main() {
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

    val periodeKafkaProducer = buildKafkaProducer(
        "${applicationConfig.perioderGroupId}-producer",
        kafkaConfig,
        PeriodeSerializer()
    )
    val opplysningerKafkaProducer = buildKafkaProducer(
        "${applicationConfig.opplysningerGroupId}-producer",
        kafkaConfig,
        OpplysningerOmArbeidssoekerSerializer()
    )
    val profileringKafkaProducer = buildKafkaProducer(
        "${applicationConfig.profileringGroupId}-producer",
        kafkaConfig,
        ProfileringSerializer()
    )
    val bekreftelseKafkaProducer = buildKafkaProducer(
        "${applicationConfig.bekreftelseGroupId}-producer",
        kafkaConfig,
        BekreftelseSerializer()
    )

    val perioder = mapOf(
        TestData.kafkaKey1 to TestData.nyStartetPeriode(identitetsnummer = TestData.fnr1)
    )

    val opplysninger = perioder.mapValues { (_, value) ->
        TestData.nyOpplysningerOmArbeidssoeker(periodeId = value.id)
    }

    val profileringer = opplysninger.mapValues { (_, value) ->
        TestData.nyProfilering(periodeId = value.periodeId, opplysningerId = value.id)
    }

    val bekreftelser = perioder.mapValues { (_, value) ->
        TestData.nyBekreftelse(periodeId = value.id)
    }

    try {
        perioder.forEach { (key, value) ->
            logger.info("Sender periode {}", value.id)
            periodeKafkaProducer.sendRecord(applicationConfig.perioderTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send periode feilet", e)
        periodeKafkaProducer.close()
    }

    try {
        opplysninger.forEach { (key, value) ->
            logger.info("Sender opplysninger for periode {}", value.periodeId)
            opplysningerKafkaProducer.sendRecord(applicationConfig.opplysningerTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send opplysninger feilet", e)
        opplysningerKafkaProducer.close()
    }

    try {
        profileringer.forEach { (key, value) ->
            logger.info("Sender profilering for periode {}", value.periodeId)
            profileringKafkaProducer.sendRecord(applicationConfig.profileringTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send profilering feilet", e)
        profileringKafkaProducer.close()
    }

    try {
        bekreftelser.forEach { (key, value) ->
            logger.info("Sender bekreftelse for periode {}", value.periodeId)
            bekreftelseKafkaProducer.sendRecord(applicationConfig.bekreftelseTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send bekreftelse feilet", e)
        bekreftelseKafkaProducer.close()
    }
}

private fun <T : SpecificRecord> Producer<Long, T>.sendRecord(
    topic: String,
    key: Long,
    value: T
): RecordMetadata? {
    return send(ProducerRecord(topic, key, value)).get()
}

private fun <T : SpecificRecord> buildKafkaProducer(
    producerId: String,
    kafkaConfig: KafkaConfig,
    valueSerializer: SpecificAvroSerializer<T>
): Producer<Long, T> {
    val kafkaFactory = KafkaFactory(kafkaConfig)

    return kafkaFactory.createProducer<Long, T>(
        clientId = producerId,
        keySerializer = LongSerializer::class,
        valueSerializer = valueSerializer::class
    )
}
