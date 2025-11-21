package no.nav.paw.oppslagapi.test

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

fun main() {
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val producerIdPrefix = "oppslag-api-v2"
    val periodeProducerId = "$producerIdPrefix-periode-producer"
    val opplysningerProducerId = "$producerIdPrefix-opplysninger-producer"
    val profileringProducerId = "$producerIdPrefix-profilering-producer"
    val egenvurderingProducerId = "$producerIdPrefix-egenvurdering-producer"
    val bekreftelseProducerId = "$producerIdPrefix-bekreftelse-producer"
    val paaVegneAvProducerId = "$producerIdPrefix-paavegneav-producer"
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

    val periodeKafkaProducer = buildKafkaProducer(
        periodeProducerId,
        kafkaConfig,
        PeriodeSerializer()
    )
    val opplysningerKafkaProducer = buildKafkaProducer(
        opplysningerProducerId,
        kafkaConfig,
        OpplysningerOmArbeidssoekerSerializer()
    )
    val profileringKafkaProducer = buildKafkaProducer(
        profileringProducerId,
        kafkaConfig,
        ProfileringSerializer()
    )
    val egenvurderingKafkaProducer = buildKafkaProducer(
        egenvurderingProducerId,
        kafkaConfig,
        EgenvurderingSerializer()
    )
    val bekreftelseKafkaProducer = buildKafkaProducer(
        bekreftelseProducerId,
        kafkaConfig,
        BekreftelseSerializer()
    )
    val paaVegneAvKafkaProducer = buildKafkaProducer(
        paaVegneAvProducerId,
        kafkaConfig,
        PaaVegneAvSerializer()
    )

    val key = -1003L
    val hendelser = TestData.hendelser1

    val perioder = hendelser
        .filter { it is Periode }
        .map { it as Periode }
        .map { key to it }

    val opplysninger = hendelser
        .filter { it is OpplysningerOmArbeidssoeker }
        .map { it as OpplysningerOmArbeidssoeker }
        .map { key to it }

    val profileringer = hendelser
        .filter { it is Profilering }
        .map { it as Profilering }
        .map { key to it }

    val egenvurderinger = hendelser
        .filter { it is Egenvurdering }
        .map { it as Egenvurdering }
        .map { key to it }

    val bekreftelser = hendelser
        .filter { it is Bekreftelse }
        .map { it as Bekreftelse }
        .map { key to it }

    val paaVegneAv = hendelser
        .filter { it is PaaVegneAv }
        .map { it as PaaVegneAv }
        .map { key to it }

    try {
        perioder.forEach { (key, value) ->
            logger.info("Sender periode {}", value.id)
            periodeKafkaProducer.sendRecord(topicNames.periodeTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send periode feilet", e)
    } finally {
        periodeKafkaProducer.close()
    }

    try {
        opplysninger.forEach { (key, value) ->
            logger.info("Sender opplysninger for periode {}", value.periodeId)
            opplysningerKafkaProducer.sendRecord(topicNames.opplysningerTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send opplysninger feilet", e)
    } finally {
        opplysningerKafkaProducer.close()
    }

    try {
        profileringer.forEach { (key, value) ->
            logger.info("Sender profilering for periode {}", value.periodeId)
            profileringKafkaProducer.sendRecord(topicNames.profileringTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send profilering feilet", e)
    } finally {
        profileringKafkaProducer.close()
    }

    try {
        egenvurderinger.forEach { (key, value) ->
            logger.info("Sender egenvurdering for periode {}", value.periodeId)
            egenvurderingKafkaProducer.sendRecord(topicNames.egenvurderingTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send egenvurdering feilet", e)
    } finally {
        egenvurderingKafkaProducer.close()
    }

    try {
        bekreftelser.forEach { (key, value) ->
            logger.info("Sender bekreftelse for periode {}", value.periodeId)
            bekreftelseKafkaProducer.sendRecord(topicNames.bekreftelseTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send bekreftelse feilet", e)
    } finally {
        bekreftelseKafkaProducer.close()
    }

    try {
        paaVegneAv.forEach { (key, value) ->
            logger.info("Sender paaVegneAv for periode {}", value.periodeId)
            paaVegneAvKafkaProducer.sendRecord(topicNames.paavnegneavTopic, key, value)
        }
    } catch (e: Exception) {
        logger.error("Send paaVegneAv feilet", e)
    } finally {
        paaVegneAvKafkaProducer.close()
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

class PeriodeSerializer : SpecificAvroSerializer<Periode>()
class OpplysningerOmArbeidssoekerSerializer : SpecificAvroSerializer<OpplysningerOmArbeidssoeker>()
class ProfileringSerializer : SpecificAvroSerializer<Profilering>()
class EgenvurderingSerializer : SpecificAvroSerializer<Egenvurdering>()
class BekreftelseSerializer : SpecificAvroSerializer<Bekreftelse>()
class PaaVegneAvSerializer : SpecificAvroSerializer<PaaVegneAv>()
