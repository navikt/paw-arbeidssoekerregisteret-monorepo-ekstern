package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.plugins.buildKafkaStreams
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.arbeidssoekerregisteret.topology.buildPeriodeTopology
import no.nav.paw.arbeidssoekerregisteret.topology.buildSiste14aVedtakTopology
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyOrNullBlocking
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import org.apache.kafka.streams.KafkaStreams

data class ApplicationContext(
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val authorizationService: AuthorizationService,
    val toggleService: ToggleService,
    val periodeKafkaStreams: KafkaStreams,
    val siste14aVedtakKafkaStreams: KafkaStreams
) {
    companion object {
        fun create(applicationConfig: ApplicationConfig): ApplicationContext {
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val azureM2MTokenClient = azureAdM2MTokenClient(
                applicationConfig.runtimeEnvironment,
                applicationConfig.azureM2M
            )

            val kafkaKeysClient = kafkaKeysClient(applicationConfig.kafkaKeysClient) {
                azureM2MTokenClient.createMachineToMachineToken(applicationConfig.kafkaKeysClient.scope)
            }

            val authorizationService = AuthorizationService()
            val toggleService = ToggleService(applicationConfig, kafkaKeysClient::getIdAndKey)

            val periodeTopology = buildPeriodeTopology(
                applicationConfig, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyBlocking
            )
            val periodeKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaStreams.periodeStreamIdSuffix,
                applicationConfig.kafka,
                healthIndicatorRepository,
                periodeTopology
            )
            val siste14aVedtakTopology = buildSiste14aVedtakTopology(
                // TODO Vil denne returnere null ved normal operasjon?
                applicationConfig, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyOrNullBlocking
            )
            val siste14aVedtakKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaStreams.siste14aVedtakStreamIdSuffix,
                applicationConfig.kafka,
                healthIndicatorRepository,
                siste14aVedtakTopology
            )

            return ApplicationContext(
                applicationConfig,
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
