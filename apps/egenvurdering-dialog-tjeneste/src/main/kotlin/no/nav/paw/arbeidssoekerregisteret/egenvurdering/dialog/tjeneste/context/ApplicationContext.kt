package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service.DialogService
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.SecureLogger
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.GenericLivenessProbe
import no.nav.paw.health.probes.databaseIsAliveCheck
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.texas.TexasClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val meterRegistry: PrometheusMeterRegistry,
    val egenvurderingAvroDeserializer: Deserializer<Egenvurdering>,
    val consumer: KafkaConsumer<Long, Egenvurdering>,
    val dialogService: DialogService,
    val dataSource: DataSource,
    val kafkaConsumerLivenessProbe: GenericLivenessProbe,
    val healthChecks: HealthChecks,
) {

    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val kafkaFactory = KafkaFactory(kafkaConfig)
            val egenvurderingAvroDeserializer: Deserializer<Egenvurdering> = kafkaFactory.kafkaAvroDeSerializer()
            val egenvurderingConsumer = kafkaFactory.createConsumer<Long, Egenvurdering>(
                groupId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                clientId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = egenvurderingAvroDeserializer::class,

                )
            val texasClient = TexasClient(applicationConfig.texasClientConfig, createHttpClient())
            val veilarbdialogClient = VeilarbdialogClient(
                config = applicationConfig.veilarbdialogClientConfig,
                texasClient = texasClient,
            )
            val dialogService = DialogService(
                veilarbdialogClient = veilarbdialogClient
            )
            val dataSource = createDataSource()
            val kafkaConsumerLivenessProbe = GenericLivenessProbe()
            val healthChecks = healthChecksOf(
                kafkaConsumerLivenessProbe,
                databaseIsAliveCheck(dataSource)
            )
            prometheusMeterRegistry.gauge(
                "${PeriodeIdDialogIdTable.tableName.lowercase()}_antall_rader",
                dataSource
            ) { _ ->
                transaction {
                    PeriodeIdDialogIdTable.selectAll()
                        .count()
                        .toDouble()
                }
            }
            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                egenvurderingAvroDeserializer = egenvurderingAvroDeserializer,
                consumer = egenvurderingConsumer,
                dialogService = dialogService,
                dataSource = dataSource,
                healthChecks = healthChecks,
                kafkaConsumerLivenessProbe = kafkaConsumerLivenessProbe
            )
        }

        private fun createDataSource() = try {
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            createHikariDataSource(
                databaseConfig,
                // grunnet feilmeldinger som: org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists
                useServerPreparedStatements = false
            )
        } catch (e: Exception) {
            SecureLogger.error("Kunne ikke opprette datasource", e)
            throw KunneIkkeOppretteDatasource("Feil ved oppsett av datasource. Se sikkerlogger for mer info")
        }

        class KunneIkkeOppretteDatasource(message: String) : RuntimeException(message)
    }
}
