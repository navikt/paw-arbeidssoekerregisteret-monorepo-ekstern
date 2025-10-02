package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.currentRuntimeEnvironment
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
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.texas.TexasClient
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val authorizationService: AuthorizationService,
    val kafkaKeysClient: KafkaKeysClient,
    val egenvurderingService: EgenvurderingService,
    val egenvurderingAvroSerializer: Serializer<Egenvurdering>,
    val producer: Producer<Long, Egenvurdering>,
    val datasource: DataSource,
    val consumer: KafkaConsumer<Long, SpecificRecord>,
    val topics: TopicNames,
    val kafkaConsumerLivenessProbe: GenericLivenessProbe,
    val healthChecks: HealthChecks
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaKeysClient = createKafkaKeyGeneratorClient()
            val texasClient = TexasClient(
                applicationConfig.texasClientConfig,
                httpClient = createHttpClient(),
            )

            val authorizationService = AuthorizationService()

            val kafkaFactory = KafkaFactory(
                kafkaConfig
            )
            val egenvurderingAvroSerializer: Serializer<Egenvurdering> = kafkaFactory.kafkaAvroSerializer()
            val egenvurderingProducer = kafkaFactory.createProducer<Long, Egenvurdering>(
                clientId = "${applicationConfig.producerConfig.applicationIdPrefix}_${applicationConfig.producerConfig.producerVersion}",
                keySerializer = LongSerializer::class,
                valueSerializer = egenvurderingAvroSerializer::class,
            )

            val deserializer: Deserializer<SpecificRecord> = kafkaFactory.kafkaAvroDeSerializer()
            val consumer: KafkaConsumer<Long, SpecificRecord> = kafkaFactory.createConsumer<Long, SpecificRecord>(
                groupId = "${applicationConfig.producerConfig.applicationIdPrefix}_${applicationConfig.producerConfig.producerVersion}",
                clientId = "${applicationConfig.producerConfig.applicationIdPrefix}_${applicationConfig.producerConfig.producerVersion}",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = deserializer::class,
            )

            val topics = standardTopicNames(currentRuntimeEnvironment)

            val egenvurderingService = EgenvurderingService(
                applicationConfig,
                kafkaKeysClient,
                egenvurderingProducer
            )
            val kafkaKafkaConsumerLivenessProbe = GenericLivenessProbe()
            val datasource = createDataSource()
            val healthChecks = healthChecksOf(
                kafkaKafkaConsumerLivenessProbe,
                databaseIsAliveCheck(datasource)
            )
            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                prometheusMeterRegistry = prometheusMeterRegistry,
                authorizationService = authorizationService,
                kafkaKeysClient = kafkaKeysClient,
                egenvurderingService = egenvurderingService,
                egenvurderingAvroSerializer = egenvurderingAvroSerializer,
                producer = egenvurderingProducer,
                datasource = datasource,
                consumer = consumer,
                topics = topics,
                healthChecks = healthChecks,
                kafkaConsumerLivenessProbe = kafkaKafkaConsumerLivenessProbe
            )
        }

        private fun createDataSource(): DataSource = try {
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            createHikariDataSource(
                databaseConfig,
                // grunnet feilmeldinger som: org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists
                useServerPreparedStatements = false
            )
        } catch (e: Exception) {
            throw KunneIkkeOppretteDatasource("Feil ved oppsett av datasource. Exception kastet: ${(e.cause ?: e)::class.simpleName}")
        }

        class KunneIkkeOppretteDatasource(message: String) : RuntimeException(message)
    }
}
