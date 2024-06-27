package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentAppName
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.time.Duration

const val APPLICATION_LOGGER_NAME = "no.nav.paw.application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"

data class AppConfig(
    val authProviders: AuthProviders,
    val azureM2M: AzureM2MConfig,
    val kafka: KafkaConfig,
    val kafkaProducer: KafkaProducerConfig,
    val kafkaStreams: KafkaStreamsConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    val regler: ReglerConfig,
    val microfrontends: MicrofrontendsConfig,
    val appName: String = currentAppName ?: "paw-microfrontend-toggler",
    val appId: String = currentAppId ?: "paw-microfrontend-toggler:LOCAL",
    val naisEnv: NaisEnv = currentNaisEnv
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val requiredClaims: RequiredClaims
)

typealias AuthProviders = List<AuthProvider>

data class RequiredClaims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)

data class KafkaProducerConfig(
    val toggleProducerIdSuffix: String,
)

data class KafkaStreamsConfig(
    val shutDownTimeout: Duration,
    val periodeStreamIdSuffix: String,
    val siste14aVedtakStreamIdSuffix: String,
    val periodeTopic: String,
    val siste14aVedtakTopic: String,
    val beriket14aVedtakTopic: String,
    val rapporteringTopic: String,
    val microfrontendTopic: String,
    val periodeStoreName: String,
    val siste14aVedtakPartitionCount: Int
)

data class ReglerConfig(
    val periodeTogglePunctuatorSchedule: Duration,
    val utsattDeaktiveringAvAiaMinSide: Duration
)

data class MicrofrontendsConfig(
    val aiaMinSide: String,
    val aiaBehovsvurdering: String
)
