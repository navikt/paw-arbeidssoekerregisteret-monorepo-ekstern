package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.time.Duration

const val APPLICATION_LOGGER_NAME = "Application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"

val currentAppName get() = "paw-microfrontend-toggler" // TODO Bruke miljøvar?

data class AppConfig(
    val authProviders: AuthProviders,
    val azureM2M: AzureM2MConfig,
    val poaoClientConfig: ServiceClientConfig,
    val kafka: KafkaConfig,
    val kafkaProducer: KafkaProducerConfig,
    val kafkaStreams: KafkaStreamsConfig,
    val kafkaKeys: KafkaKeyConfig,
    val regler: ReglerConfig,
    val microfrontends: MicrofrontendsConfig,
    val appName: String = currentAppName,
    val appId: String = currentAppId,
    val naisEnv: NaisEnv = currentNaisEnv
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: Claims
)

typealias AuthProviders = List<AuthProvider>

data class Claims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class KafkaProducerConfig(
    val applicationIdSuffix: String,
)

data class KafkaStreamsConfig(
    val applicationIdSuffix: String,
    val periodeTopic: String,
    val vedtakTopic: String,
    val microfrontendTopic: String,
    val toggleStoreName: String,
    val varselStoreName: String,
    val periodeToggleProcessor: String,
    val rapporteringToggleProcessor: String
)

data class ReglerConfig(
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)

data class MicrofrontendsConfig(
    val aiaMinSide: String,
    val aiaBehovsvurdering: String
)