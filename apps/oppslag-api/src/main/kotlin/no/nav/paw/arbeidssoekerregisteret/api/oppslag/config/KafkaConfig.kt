package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.LongDeserializer

data class KafkaConfig(
    val periodeTopic: String,
    val opplysningerOmArbeidssoekerTopic: String,
    val profileringTopic: String,
    val gruppeId: String,
    val serverConfig: KafkaServerConfig,
    val schemaRegistryConfig: SchemaRegistryConfig
)

val KafkaConfig.properties
    get(): Map<String, Any?> =
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to gruppeId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to serverConfig.kafkaBrokers,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 200,
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryConfig.url,
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to schemaRegistryConfig.autoRegistrerSchema,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to LongDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java.name
        ) + (
            schemaRegistryConfig.bruker?.let {
                mapOf(
                    SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                    SchemaRegistryClientConfig.USER_INFO_CONFIG to "${schemaRegistryConfig.bruker}:${schemaRegistryConfig.passord}"
                )
            } ?: emptyMap<String, Any?>()
        ) +
            if (serverConfig.autentisering.equals("SSL", true)) {
                mapOf(
                    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
                    SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to serverConfig.keystorePath,
                    SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to serverConfig.credstorePassword,
                    SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to serverConfig.truststorePath,
                    SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to serverConfig.credstorePassword
                )
            } else {
                emptyMap()
            }

fun <T : SpecificRecord> KafkaConfig.createKafkaConsumer(): KafkaConsumer<Long, T> {
    return KafkaConsumer<Long, T>(properties)
}
