package no.nav.paw.ledigestillinger.context

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.HealthChecks
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import javax.sql.DataSource

data class ApplicationContext(
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val databaseConfig: DatabaseConfig = loadNaisOrLocalConfiguration(DATABASE_CONFIG),
    //val dataSource: DataSource = createHikariDataSource(databaseConfig),
    val healthChecks: HealthChecks = HealthChecks(),
    val meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    val meterBinders: List<MeterBinder> = emptyList(),
    val kafkaFactory: KafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
)
