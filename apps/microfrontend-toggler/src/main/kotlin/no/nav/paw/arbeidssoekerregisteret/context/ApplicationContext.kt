package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.topology.buildPeriodeTopology
import no.nav.paw.arbeidssoekerregisteret.utils.buildKafkaStreams
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.KafkaStreamsHealthProbe
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafkakeygenerator.factory.createKafkaKeyGeneratorClient
import org.apache.kafka.streams.KafkaStreams

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val periodeKafkaStreams: KafkaStreams,
    val healthChecks: HealthChecks
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaKeysClient = createKafkaKeyGeneratorClient()

            val periodeTopology = buildPeriodeTopology(
                applicationConfig, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyBlocking
            )
            val periodeKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaTopology.periodeStreamIdSuffix,
                kafkaConfig,
                periodeTopology
            )
            val healthChecks = healthChecksOf(
                KafkaStreamsHealthProbe(periodeKafkaStreams),
            )

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                prometheusMeterRegistry,
                periodeKafkaStreams,
                healthChecks
            )
        }
    }
}
