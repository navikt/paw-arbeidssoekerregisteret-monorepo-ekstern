import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.kafka.RapporteringTilgjengeligState
import no.nav.paw.rapportering.api.kafka.RapporteringTilgjengeligStateSerde
import no.nav.paw.rapportering.api.kafka.appTopology
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.apache.kafka.streams.state.Stores

fun createDependencies(
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    kafkaStreamsConfig: KafkaConfig,
    azureM2MConfig: AzureM2MConfig,
    kafkaKeyConfig: KafkaKeyConfig
): Dependencies {
    val azureM2MTokenClient = azureAdM2MTokenClient(applicationConfig.naisEnv, azureM2MConfig)
    val kafkaKeyClient = kafkaKeysKlient(kafkaKeyConfig) {
        azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
    }

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val streamsConfig = KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaStreamsConfig)
        .withDefaultKeySerde(Serdes.LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.stateStoreName),
                Serdes.Long(),
                RapporteringTilgjengeligStateSerde(),
            )
        )

    val topology = streamsBuilder.appTopology(
        prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        rapporteringHendelseLoggTopic = applicationConfig.rapporteringHendelseLoggTopic,
        stateStoreName = applicationConfig.stateStoreName,
    )

    val kafkaStreams = KafkaStreams(
        topology,
        streamsConfig.properties
    )

    val kafkaStreamsStore: ReadOnlyKeyValueStore<Long, RapporteringTilgjengeligState> = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            applicationConfig.stateStoreName,
            QueryableStoreTypes.keyValueStore()
        )
    )

    return Dependencies(
        kafkaKeyClient,
        httpClient,
        kafkaStreams,
        prometheusMeterRegistry,
        kafkaStreamsStore
    )
}

data class Dependencies(
    val kafkaKeyClient: KafkaKeysClient,
    val httpClient: HttpClient,
    val kafkaStreams: KafkaStreams,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val kafkaStreamsStore: ReadOnlyKeyValueStore<Long, RapporteringTilgjengeligState>,
)