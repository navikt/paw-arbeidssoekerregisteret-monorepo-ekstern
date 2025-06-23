package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.texas.TexasClient
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.EgenvurderingService
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val authorizationService: AuthorizationService,
    val kafkaKeysClient: KafkaKeysClient,
    val egenvurderingService: EgenvurderingService,
    val egenvurderingAvroSerializer: Serializer<Egenvurdering>,
    val producer: Producer<Long, Egenvurdering>
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val kafkaKeysClient = createKafkaKeyGeneratorClient()
            val texasClient = TexasClient(
                applicationConfig.texasClientConfig
            )

            val oppslagsClient = ApiOppslagClient(applicationConfig.oppslagApiConfig.url)

            val authorizationService = AuthorizationService()

            val kafkaFactory = KafkaFactory(
                kafkaConfig
            )
            val egenvurderingAvroSerializer: Serializer<Egenvurdering> = kafkaFactory.kafkaAvroSerializer()
            val egenvurderingProducer = kafkaFactory.createProducer<Long, Egenvurdering>(
                clientId = "${applicationConfig.kafkaTopology.applicationIdPrefix}_${applicationConfig.kafkaTopology.producerVersion}",
                keySerializer = LongSerializer::class,
                valueSerializer = egenvurderingAvroSerializer::class,
            )

            val egenvurderingService = EgenvurderingService(applicationConfig, kafkaKeysClient, egenvurderingProducer, texasClient, oppslagsClient)


            return ApplicationContext(
                serverConfig,
                applicationConfig,
                securityConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                authorizationService,
                kafkaKeysClient,
                egenvurderingService,
                egenvurderingAvroSerializer,
                egenvurderingProducer
            )
        }
    }
}
