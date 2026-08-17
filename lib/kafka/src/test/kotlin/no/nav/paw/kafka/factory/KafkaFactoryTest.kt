package no.nav.paw.kafka.factory

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.kafka.config.KafkaAuthenticationConfig
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.config.KafkaSchemaRegistryConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerInterceptor
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile

class RecordingConsumerInterceptor : ConsumerInterceptor<Any, Any> {
    override fun configure(configs: Map<String, *>) {
        configuredProperties = configs
    }

    override fun onConsume(records: ConsumerRecords<Any, Any>): ConsumerRecords<Any, Any> = records

    override fun onCommit(offsets: Map<TopicPartition, OffsetAndMetadata>) = Unit

    override fun close() = Unit

    companion object {
        var configuredProperties: Map<String, *> = emptyMap<String, Any?>()
    }
}

class KafkaFactoryTest : StringSpec({
    "setter riktig security protocol" {
        val truststoreFile = createTempFile("truststore", ".jks")
        val keystoreFile = createTempFile("keystore", ".jks")
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                authentication =
                    KafkaAuthenticationConfig(
                        truststorePath = truststoreFile.absolutePathString(),
                        keystorePath = keystoreFile.absolutePathString(),
                        credstorePassword = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["security.protocol"] shouldBe "SSL"
    }
    "setter riktig schema registry url" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                schemaRegistry =
                    KafkaSchemaRegistryConfig(
                        url = "http://localhost:8081",
                        username = "username",
                        password = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["schema.registry.url"] shouldBe "http://localhost:8081"
    }
    "setter riktig schema registry basic auth" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                schemaRegistry =
                    KafkaSchemaRegistryConfig(
                        url = "http://localhost:8081",
                        username = "username",
                        password = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["basic.auth.credentials.source"] shouldBe "USER_INFO"
        factory.baseProperties["basic.auth.user.info"] shouldBe "username:password"
    }
    "lager kafka producer" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092"
            )

        val factory = KafkaFactory(config)

        val producer =
            factory.createProducer(
                "testId",
                Serdes.String().serializer()::class,
                Serdes.String().serializer()::class
            )

        producer.javaClass shouldBe KafkaProducer::class.java
    }
    "lager kafka consumer" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092"
            )

        val factory = KafkaFactory(config)

        val consumer =
            factory.createConsumer(
                groupId = "groupId",
                clientId = "clientId",
                keyDeserializer = Serdes.String().deserializer()::class,
                valueDeserializer = Serdes.String().deserializer()::class
            )
        consumer.javaClass shouldBe KafkaConsumer::class.java
    }
    "producer-interceptor i producerExtraProperties lekker ikke til consumer" {
        val config = KafkaConfig(
            brokers = "localhost:9092",
            producerExtraProperties = mapOf(
                "interceptor.classes" to "no.nav.paw.kafka.signing.SigningProducerInterceptor"
            )
        )

        val factory = KafkaFactory(config)

        // Consumer skal opprettes uten feil selv om producerExtraProperties inneholder en producer-interceptor
        val consumer = factory.createConsumer(
            groupId = "groupId",
            clientId = "clientId",
            keyDeserializer = Serdes.String().deserializer()::class,
            valueDeserializer = Serdes.String().deserializer()::class
        )
        consumer.javaClass shouldBe KafkaConsumer::class.java
    }
    "bruker consumerExtraProperties med deserializer-klasser" {
        val config = KafkaConfig(
            brokers = "localhost:9092",
            consumerExtraProperties = mapOf(
                "interceptor.classes" to RecordingConsumerInterceptor::class.java.name,
                "test.consumer.property" to "class-deserializer",
                ConsumerConfig.GROUP_ID_CONFIG to "skal-overstyres",
            )
        )

        val consumer = KafkaFactory(config).createConsumer(
            groupId = "groupId",
            clientId = "clientId",
            keyDeserializer = Serdes.String().deserializer()::class,
            valueDeserializer = Serdes.String().deserializer()::class
        )

        RecordingConsumerInterceptor.configuredProperties["test.consumer.property"] shouldBe "class-deserializer"
        RecordingConsumerInterceptor.configuredProperties[ConsumerConfig.GROUP_ID_CONFIG] shouldBe "groupId"
        consumer.close()
    }
    "bruker consumerExtraProperties med deserializer-instanser" {
        val config = KafkaConfig(
            brokers = "localhost:9092",
            consumerExtraProperties = mapOf(
                "interceptor.classes" to RecordingConsumerInterceptor::class.java.name,
                "test.consumer.property" to "deserializer-instance"
            )
        )

        val consumer = KafkaFactory(config).createConsumer(
            groupId = "groupId",
            clientId = "clientId",
            keyDeserializer = Serdes.String().deserializer(),
            valueDeserializer = Serdes.String().deserializer()
        )

        RecordingConsumerInterceptor.configuredProperties["test.consumer.property"] shouldBe "deserializer-instance"
        consumer.close()
    }
})
