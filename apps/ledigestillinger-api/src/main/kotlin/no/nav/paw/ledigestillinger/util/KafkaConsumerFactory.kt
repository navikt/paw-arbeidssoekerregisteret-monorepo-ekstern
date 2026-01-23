package no.nav.paw.ledigestillinger.util

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.hwm.HwmTopicConfig
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.asMessageConsumerWithHwmAndMetrics
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.ledigestillinger.config.KafkaConsumerConfig
import no.nav.paw.ledigestillinger.consumer.HwmMessageConsumer
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

fun <K : Any, V : Any> KafkaFactory.createHwmKafkaConsumer(
    meterRegistry: PrometheusMeterRegistry,
    consumerConfig: KafkaConsumerConfig,
    keyDeserializer: KClass<out Deserializer<K>>,
    valueDeserializer: KClass<out Deserializer<V>>,
    consumeFunction: (Sequence<Message<K, V>>) -> Unit
): HwmMessageConsumer<K, V> = createConsumer(
    clientId = consumerConfig.clientId,
    groupId = consumerConfig.groupId,
    keyDeserializer = keyDeserializer,
    valueDeserializer = valueDeserializer
).asMessageConsumerWithHwmAndMetrics(
    prometheusMeterRegistry = meterRegistry,
    receiver = consumeFunction,
    hwmTopicConfig = listOf(
        HwmTopicConfig(
            topic = consumerConfig.topic,
            consumerVersion = consumerConfig.version
        )
    )
)