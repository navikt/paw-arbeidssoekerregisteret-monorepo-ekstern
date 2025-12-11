package no.naw.paw.minestillinger

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.kafka.factory.KafkaFactory
import no.naw.paw.minestillinger.vedtak14a.Siste14aDeserializer
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class TopicAwareDeserializer(
    val map: Map<String, Deserializer<*>>
): Deserializer<Any> {
    override fun deserialize(topic: String, p1: ByteArray): Any {
        return map[topic].let { deserializer ->
            if (deserializer == null) {
                throw IllegalArgumentException("No deserializer found for topic $topic")
            } else {
                deserializer.deserialize(topic, p1)
            }
        }
    }
}

fun initTopicAwareKeyDeserializer(
    topicNames: TopicNames,
    siste14aVedtakTopic: String
): TopicAwareDeserializer {

    val deserializerMap: Map<String, Deserializer<*>> = mapOf(
        topicNames.periodeTopic to LongDeserializer(),
        topicNames.profileringTopic to LongDeserializer(),
        siste14aVedtakTopic to StringDeserializer()
    )
    return TopicAwareDeserializer(deserializerMap)
}

fun KafkaFactory.initTopicAwareValueDeserializer(
    topicNames: TopicNames,
    siste14aVedtakTopic: String
): TopicAwareDeserializer {
    val specificAvroDeserializer = kafkaAvroDeSerializer<SpecificRecord>()
    val deserializerMap: Map<String, Deserializer<*>> = mapOf(
        topicNames.periodeTopic to specificAvroDeserializer,
        topicNames.profileringTopic to specificAvroDeserializer,
        siste14aVedtakTopic to Siste14aDeserializer
    )
    return TopicAwareDeserializer(deserializerMap)
}