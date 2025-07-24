package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.context

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.services.DialogService
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRepository
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val egenvurderingAvroDeserializer: Deserializer<Egenvurdering>,
    val consumer: KafkaConsumer<Long, Egenvurdering>,
    val egenvurderingConsumerWrapper: CommittingKafkaConsumerWrapper<Long, Egenvurdering>,
    val dialogService: DialogService,
    val dataSource: HikariDataSource,
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val dataSource = createHikariDataSource(databaseConfig)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val kafkaFactory = KafkaFactory(
                kafkaConfig
            )
            val egenvurderingAvroDeserializer: Deserializer<Egenvurdering> = kafkaFactory.kafkaAvroDeSerializer()
            val egenvurderingConsumer = kafkaFactory.createConsumer<Long, Egenvurdering>(
                groupId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                clientId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.consumerVersion}",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = egenvurderingAvroDeserializer::class,
            )
            val egenvurderingConsumerWrapper = CommittingKafkaConsumerWrapper<Long, Egenvurdering>(
                topics = listOf(applicationConfig.kafkaTopology.egenvurderingTopic),
                consumer = egenvurderingConsumer,
            )

            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig(applicationConfig.veilarbdialogClientConfig.url, applicationConfig.veilarbdialogClientConfig.scope),
            )
            val dialogRepository = DialogRepository()
            val dialogService = DialogService(
                veilarbdialogClient = veilarbdialogClient,
                dialogRepository = dialogRepository,
            )

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                egenvurderingAvroDeserializer,
                egenvurderingConsumer,
                egenvurderingConsumerWrapper,
                dialogService,
                dataSource,
            )
        }
    }
}
