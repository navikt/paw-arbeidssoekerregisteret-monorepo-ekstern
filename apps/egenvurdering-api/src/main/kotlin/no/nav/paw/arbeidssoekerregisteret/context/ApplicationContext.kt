package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.topology.buildProfileringTopology
import no.nav.paw.arbeidssoekerregisteret.utils.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildKafkaStreams
import no.nav.paw.arbeidssoekerregisteret.utils.getIdAndKeyBlocking
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val authorizationService: AuthorizationService,
    val kafkaKeysClient: KafkaKeysClient,
    val egenvurderingService: EgenvurderingService,
    val periodeSerde: Serde<Periode>,
    val profileringSerde: Serde<Profilering>,
    val egenvurderingSerde: Serde<Egenvurdering>,
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

            val kafkaStreamsFactory = KafkaStreamsFactory(
                applicationConfig.kafkaTopology.applicationId,
                kafkaConfig
            )

            val periodeSerde = kafkaStreamsFactory.createSpecificAvroSerde<Periode>()
            val profileringSerde = kafkaStreamsFactory.createSpecificAvroSerde<Profilering>()
            val egenvurderingSerde = kafkaStreamsFactory.createSpecificAvroSerde<Egenvurdering>()

            val profileringTopology = buildProfileringTopology(applicationConfig, profileringSerde, prometheusMeterRegistry, kafkaKeysClient::getIdAndKeyBlocking)
            val profileringKafkaStreams = buildKafkaStreams(
                applicationConfig.kafkaTopology.periodeStreamIdSuffix,
                kafkaConfig,
                healthIndicatorRepository, // TODO: bytt ut med nye health lib
                profileringTopology
            )
            val storeSupplier = StoreQueryParameters.fromNameAndType(
                applicationConfig.kafkaTopology.profileringStateStoreName,
                QueryableStoreTypes.keyValueStore<Long, Profilering>()
            )

            val profileringStateStore: ReadOnlyKeyValueStore<Long, Profilering> =
                profileringKafkaStreams.store(storeSupplier)

            val egenvurderingService = EgenvurderingService(applicationConfig, kafkaConfig, kafkaKeysClient, profileringStateStore)


            return ApplicationContext(
                serverConfig,
                applicationConfig,
                securityConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                authorizationService,
                kafkaKeysClient,
                egenvurderingService,
                periodeSerde,
                profileringSerde,
                egenvurderingSerde
            )
        }
    }
}
