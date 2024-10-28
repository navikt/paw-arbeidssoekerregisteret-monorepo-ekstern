package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.BekreftelseSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.GenericSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.OpplysningerOmArbeidssoekerSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PeriodeSerializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ProfileringSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer

fun main() {
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

    val periodeKafkaProducer = buildKafkaProducer(
        applicationConfig.consumerId,
        kafkaConfig,
        PeriodeSerializer()
    )
    val opplysningerKafkaProducer = buildKafkaProducer(
        applicationConfig.consumerId,
        kafkaConfig,
        OpplysningerOmArbeidssoekerSerializer()
    )
    val profileringKafkaProducer = buildKafkaProducer(
        applicationConfig.consumerId,
        kafkaConfig,
        ProfileringSerializer()
    )
    val bekreftelseKafkaProducer = buildKafkaProducer(
        applicationConfig.consumerId,
        kafkaConfig,
        BekreftelseSerializer()
    )

    val perioder = mapOf(
        TestData.kafkaKey1 to TestData.nyStartetPeriode()
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
            periodeKafkaProducer.sendRecord(applicationConfig.periodeTopic, key, value)
        }
    } catch (e: Exception) {
        periodeKafkaProducer.close()
    }

    try {
        opplysninger.forEach { (key, value) ->
            opplysningerKafkaProducer.sendRecord(applicationConfig.opplysningerTopic, key, value)
        }
    } catch (e: Exception) {
        opplysningerKafkaProducer.close()
    }

    try {
        profileringer.forEach { (key, value) ->
            profileringKafkaProducer.sendRecord(applicationConfig.profileringTopic, key, value)
        }
    } catch (e: Exception) {
        profileringKafkaProducer.close()
    }

    try {
        bekreftelser.forEach { (key, value) ->
            bekreftelseKafkaProducer.sendRecord(applicationConfig.bekreftelseTopic, key, value)
        }
    } catch (e: Exception) {
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
    valueSerializer: GenericSerializer<T>
): Producer<Long, T> {
    val kafkaFactory = KafkaFactory(kafkaConfig)

    return kafkaFactory.createProducer<Long, T>(
        clientId = producerId,
        keySerializer = LongSerializer::class,
        valueSerializer = valueSerializer::class
    )
}
