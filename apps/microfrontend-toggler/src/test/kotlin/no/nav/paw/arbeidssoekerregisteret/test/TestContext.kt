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
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.routes.toggleRoutes
import no.nav.paw.arbeidssoekerregisteret.service.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.service.ToggleService
import no.nav.paw.arbeidssoekerregisteret.utils.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import no.nav.paw.arbeidssoekerregisteret.utils.configureJackson
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.healthChecksOf
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

open class TestContext(
    open val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val serverConfig: ServerConfig = loadNaisOrLocalConfiguration(SERVER_CONFIG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    val meterRegistry: SimpleMeterRegistry = SimpleMeterRegistry(),
    val prometheusMeterRegistryMock: PrometheusMeterRegistry = mockk<PrometheusMeterRegistry>(),
    val kafkaKeysClientMock: KafkaKeysClient = mockk<KafkaKeysClient>(),
    val periodeSerde: Serde<Periode> = buildAvroSerde(),
    val toggleSerde: Serde<Toggle> = buildToggleSerde(),
    val authorizationService: AuthorizationService = AuthorizationService(),
    val toggleServiceMock: ToggleService = mockk<ToggleService>(),
    val kafkaStreamProperties: Properties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test-kafka-streams"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
        this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
        this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://test-registry"
    }
) {
    fun <K, V> KeyValueStore<K, V>.size(): Int {
        var count = 0
        for (keyValue in all()) {
            count++
        }
        return count
    }

    fun <K, V> KeyValueStore<K, V>.asList(): List<KeyValue<K, V>> {
        val list = mutableListOf<KeyValue<K, V>>()
        for (keyValue in all()) {
            list.add(keyValue)
        }
        return list
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
            periodeKafkaStreams = mockk<KafkaStreams>(),
            healthChecks = healthChecksOf()
        )
        application {
            configureSerialization()
            configureHTTP()
            configureAuthentication(applicationContext)
        }
        routing {
            toggleRoutes(authorizationService, applicationContext.toggleService)
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

    companion object {
        fun <T : SpecificRecord> buildAvroSerde(): Serde<T> {
            val schemaRegistryClient = MockSchemaRegistry.getClientForScope("test-registry")
            val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
            serde.configure(
                mapOf(
                    KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                    KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://test-registry"
                ),
                false
            )
            return serde
        }
    }
}

fun StreamsBuilder.addPeriodeInMemoryStateStore(applicationConfig: ApplicationConfig) {
    addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(applicationConfig.kafkaTopology.periodeStateStore),
            Serdes.Long(),
            buildPeriodeInfoSerde()
        )
    )
}