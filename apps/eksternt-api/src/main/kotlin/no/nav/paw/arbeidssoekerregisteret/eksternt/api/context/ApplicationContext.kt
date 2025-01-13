package no.nav.paw.arbeidssoekerregisteret.eksternt.api.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ScheduledTaskService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.PeriodeDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val meterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    val periodeService: PeriodeService,
    val scheduledTaskService: ScheduledTaskService
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val dataSource = createHikariDataSource(databaseConfig)
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val periodeRepository = PeriodeRepository()
            val periodeService = PeriodeService(periodeRepository)
            val scheduledTaskService = ScheduledTaskService(meterRegistry, periodeRepository)
            val kafkaFactory = KafkaFactory(kafkaConfig)

            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class
            )

            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                dataSource = dataSource,
                meterRegistry = meterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                scheduledTaskService = scheduledTaskService,
                periodeService = periodeService,
                periodeKafkaConsumer = periodeKafkaConsumer
            )
        }
    }
}
