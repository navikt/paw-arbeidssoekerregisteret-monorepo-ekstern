package no.nav.paw.arbeidssoekerregisteret.test

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
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureHTTP
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
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

open class TestContext {

    val SCHEMA_REGISTRY_SCOPE = "test-registry"

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val mockOAuth2Server = MockOAuth2Server()
    val meterRegistry = SimpleMeterRegistry()
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val periodeSerde = buildAvroSerde<Periode>()
    val periodeInfoSerde = buildPeriodeInfoSerde()
    val siste14aVedtakSerde = buildSiste14aVedtakSerde()
    val beriket14aVedtakSerde = buildBeriket14aVedtakSerde()
    val toggleSerde = buildToggleSerde()
    val prometheusMeterRegistryMock = mockk<PrometheusMeterRegistry>()
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
            serverConfig,
            applicationConfig,
            securityConfig.copy(authProviders = securityConfig.authProviders.map {
                it.copy(
                    audiences = listOf("default"),
                    discoveryUrl = mockOAuth2Server.wellKnownUrl("default").toString()
                )
            }),
            prometheusMeterRegistryMock,
            authorizationService,
            toggleServiceMock,
            mockk<KafkaStreams>(),
            mockk<KafkaStreams>()
        )
        application {
            configureSerialization()
            configureHTTP()
            configureAuthentication(applicationContext)
        }
        routing {
            toggleRoutes(applicationConfig, authorizationService, applicationContext.toggleService)
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
}
