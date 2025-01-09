package no.nav.paw.arbeidssoekerregisteret.eksternt.api.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.DATABASE_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SECURITY_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SecurityConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.PeriodeConsumer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ScheduleDeletionService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.AktivePerioderGaugeScheduler
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val meterRegistry: PrometheusMeterRegistry,
    val aktivePerioderGaugeScheduler: AktivePerioderGaugeScheduler,
    val periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    val periodeConsumer: PeriodeConsumer,
    val periodeService: PeriodeService,
    val scheduleDeletionService: ScheduleDeletionService
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val dataSource = generateDatasource(databaseConfig.url)
            val database = Database.connect(dataSource)

            val periodeRepository = PeriodeRepository(database)
            val periodeService = PeriodeService(periodeRepository)
            val scheduleDeletionService = ScheduleDeletionService(periodeRepository)
            val aktivePerioderGaugeScheduler = AktivePerioderGaugeScheduler(meterRegistry, periodeRepository)
            val kafkaFactory = KafkaFactory(kafkaConfig)

            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class
            )

            val periodeConsumer = PeriodeConsumer(applicationConfig.periodeTopic, periodeKafkaConsumer, periodeService)

            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                dataSource = dataSource,
                meterRegistry = meterRegistry,
                aktivePerioderGaugeScheduler = aktivePerioderGaugeScheduler,
                scheduleDeletionService = scheduleDeletionService,
                periodeConsumer = periodeConsumer,
                periodeService = periodeService,
                periodeKafkaConsumer = periodeKafkaConsumer
            )
        }
    }
}
