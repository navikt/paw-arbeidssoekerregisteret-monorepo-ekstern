package no.nav.paw.arbeidssoekerregisteret.config

const val APP_LOGGER_NAME = "app"
const val CONFIG_FILE_NAME = "application.yaml"

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
    val kafkaKeys: KafkaKeysConfig,
    val id: String = currentAppId
)

data class KafkaKeysConfig(
    val url: String,
    val scope: String
)
