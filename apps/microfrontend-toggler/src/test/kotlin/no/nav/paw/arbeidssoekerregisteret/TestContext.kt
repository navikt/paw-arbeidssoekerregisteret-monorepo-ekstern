package no.nav.paw.arbeidssoekerregisteret

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.AuthProvider
import no.nav.paw.arbeidssoekerregisteret.config.AuthProviders
import no.nav.paw.arbeidssoekerregisteret.config.RequiredClaims
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRequestHandling
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.arbeidssoekerregisteret.utils.buildBeriket14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildSiste14aVedtakSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.configureJackson
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

open class TestContext {

    val TEST_APPLICATION_CONFIG_FILE_NAME = "test_application_configuration.toml"
    val SCHEMA_REGISTRY_SCOPE = "test-registry"

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(TEST_APPLICATION_CONFIG_FILE_NAME)
    val mockOAuth2Server = MockOAuth2Server()
    val meterRegistry = SimpleMeterRegistry()
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val periodeSerde = buildAvroSerde<Periode>()
    val periodeInfoSerde = buildPeriodeInfoSerde()
    val siste14aVedtakSerde = buildSiste14aVedtakSerde()
    val beriket14aVedtakSerde = buildBeriket14aVedtakSerde()
    val toggleSerde = buildToggleSerde()
    val prometheusMeterRegistryMock = mockk<PrometheusMeterRegistry>()
    val healthIndicatorRepository = HealthIndicatorRepository()
    val authorizationService = AuthorizationService()
    val toggleServiceMock = mockk<ToggleService>()

    val kafkaStreamProperties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test-kafka-streams"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
        this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
        this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
    }

    fun <T : SpecificRecord> buildAvroSerde(): Serde<T> {
        val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
        val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
        serde.configure(
            mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
            ),
            false
        )
        return serde
    }

    fun <K, V> KeyValueStore<K, V>.size(): Int {
        var count = 0
        for (keyValue in all()) {
            count++
        }
        return count
    }

    fun ApplicationTestBuilder.configureTestApplication() {
        val applicationContext = ApplicationContext(
            applicationConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
            prometheusMeterRegistryMock,
            healthIndicatorRepository,
            authorizationService,
            toggleServiceMock,
            mockk<KafkaStreams>(),
            mockk<KafkaStreams>()
        )
        application {
            configureSerialization()
            configureRequestHandling()
            configureAuthentication(applicationContext)
        }
        routing {
            toggleRoutes(applicationContext)
        }
    }

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }

    fun MockOAuth2Server.createAuthProviders(): AuthProviders {
        return listOf(
            AuthProvider(
                name = "tokenx",
                clientId = "default",
                discoveryUrl = wellKnownUrl("default").toString(),
                tokenEndpointUrl = tokenEndpointUrl("default").toString(),
                requiredClaims = RequiredClaims(listOf(), true)
            )
        )
    }
}
