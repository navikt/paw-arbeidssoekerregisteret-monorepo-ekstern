package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.DialogService
import no.nav.paw.arbeidssoekerregisteret.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

private val logger = buildApplicationLogger

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val egenvurderingAvroDeserializer: Deserializer<Egenvurdering>,
    val consumer: KafkaConsumer<Long, Egenvurdering>,
    val dialogService: DialogService,
    val dataSource: DataSource?,
) {

    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaFactory = KafkaFactory(kafkaConfig)
            val egenvurderingAvroDeserializer: Deserializer<Egenvurdering> = kafkaFactory.kafkaAvroDeSerializer()
            val egenvurderingConsumer = kafkaFactory.createConsumer<Long, Egenvurdering>(
                groupId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                clientId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = egenvurderingAvroDeserializer::class,
            )

            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig("", ""),
            )
            val dialogService = DialogService(veilarbdialogClient = veilarbdialogClient)

            val dataSource: DataSource? = createDataSource()
            logger.info("Datasource er opprettet uten feil")

            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                prometheusMeterRegistry = prometheusMeterRegistry,
                egenvurderingAvroDeserializer = egenvurderingAvroDeserializer,
                consumer = egenvurderingConsumer,
                dialogService = dialogService,
                dataSource = dataSource
            )
        }

        private fun createDataSource(): DataSource? {
            try {
                val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
                return createHikariDataSource(databaseConfig)
            } catch (e: Exception) {
                logger.error("Feil ved oppsett av datasource. Exception kastet: ${e.javaClass.simpleName}")
                return null
            }
        }
    }
}
