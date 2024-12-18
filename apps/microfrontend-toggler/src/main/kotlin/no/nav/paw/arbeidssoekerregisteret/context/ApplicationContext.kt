package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.plugins.buildKafkaStreams
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.arbeidssoekerregisteret.topology.buildPeriodeTopology
import no.nav.paw.arbeidssoekerregisteret.topology.buildSiste14aVedtakTopology
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyOrNullBlocking
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.streams.KafkaStreams

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val authorizationService: AuthorizationService,
    val toggleService: ToggleService,
    val periodeKafkaStreams: KafkaStreams,
    val siste14aVedtakKafkaStreams: KafkaStreams
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val kafkaKeysClient = createKafkaKeyGeneratorClient()

            val authorizationService = AuthorizationService()
            val toggleService = ToggleService(applicationConfig, kafkaConfig, kafkaKeysClient::getIdAndKey)

            val periodeTopology = buildPeriodeTopology(
                applicationConfig, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyBlocking
            )
            val periodeKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaTopology.periodeStreamIdSuffix,
                kafkaConfig,
                healthIndicatorRepository,
                periodeTopology
            )
            val siste14aVedtakTopology = buildSiste14aVedtakTopology(
                // TODO Vil denne returnere null ved normal operasjon?
                applicationConfig, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyOrNullBlocking
            )
            val siste14aVedtakKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaTopology.siste14aVedtakStreamIdSuffix,
                kafkaConfig,
                healthIndicatorRepository,
                siste14aVedtakTopology
            )

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                securityConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                authorizationService,
                toggleService,
                periodeKafkaStreams,
                siste14aVedtakKafkaStreams
            )
        }
    }
}
