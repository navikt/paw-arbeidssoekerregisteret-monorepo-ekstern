package no.nav.paw.arbeidssoekerregisteret.config

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig

const val APPLICATION_LOGGER_NAME = "app"
const val APPLICATION_CONFIG_FILE_NAME = "application.toml"

data class Config(
    val server: ServerConfig = ServerConfig(),
    val app: AppConfig,
    val env: Env = currentEnv
)

data class ServerConfig(
    val port: Int = 8080,
    val callGroupSize: Int = 16,
    val workerGroupSize: Int = 8,
    val connectionGroupSize: Int = 8,
    val gracePeriodMillis: Long = 300,
    val timeoutMillis: Long = 300,
)

data class AppConfig(
    val kafka: KafkaConfig,
    val kafkaKeys: KafkaKeysConfig,
    val appId: String = currentAppId
)

data class KafkaConfig(
    val appId: String,
    val brokers: String,
    val periodeTopic: String,
    val vedtakTopic: String,
    val microfrontendTopic: String,
    val toggleStoreName: String,
    val varselStoreName: String,
    val security: KafkaSecurityConfig
) {
    fun asProperties(): Map<String, Any?> {
        return listOfNotNull(
            mapOf(
                StreamsConfig.APPLICATION_ID_CONFIG to appId,
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to brokers,
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.Long().javaClass.name,
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to SpecificAvroSerde::class.java.name,
                StreamsConfig.PROCESSING_GUARANTEE_CONFIG to StreamsConfig.EXACTLY_ONCE_V2
            ),
            security.asProperties()
        ).reduce { acc, map -> acc + map }
    }
}

data class KafkaSecurityConfig(
    val protocol: String,
    val keyStorePath: String?,
    val keyStorePassword: String?,
    val trustStorePath: String?,
    val trustStorePassword: String?
) {
    fun asProperties(): Map<String, Any?> {
        return if (protocol == "SSL") {
            mapOf(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keyStorePath,
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to keyStorePassword,
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to trustStorePath,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to trustStorePassword
            )
        } else {
            mapOf(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol
            )
        }
    }
}

data class KafkaKeysConfig(
    val url: String,
    val scope: String
)
