package no.nav.paw.rapportering.api

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysKlient
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.config.AuthProviders
import no.nav.paw.rapportering.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.rapportering.api.kafka.RapporteringTilgjengeligStateSerde
import no.nav.paw.rapportering.api.kafka.appTopology
import no.nav.paw.rapportering.api.plugins.configureAuthentication
import no.nav.paw.rapportering.api.plugins.configureHTTP
import no.nav.paw.rapportering.api.plugins.configureLogging
import no.nav.paw.rapportering.api.plugins.configureMetrics
import no.nav.paw.rapportering.api.plugins.configureOtel
import no.nav.paw.rapportering.api.plugins.configureSerialization
import no.nav.paw.rapportering.api.routes.healthRoutes
import no.nav.paw.rapportering.api.routes.rapporteringRoutes
import no.nav.paw.rapportering.api.routes.swaggerRoutes
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("rapportering-api")
    logger.info("Starter: ${ApplicationInfo.id}")

    // Konfigurasjon
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>("azure_m2m_key_config.toml")
    val kafkaKeyConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>("kafka_key_generator_client_config.toml")

    // Klient initialisering
    val azureM2MTokenClient = azureAdM2MTokenClient(applicationConfig.naisEnv, azureM2MConfig)
    val kafkaKeyClient = kafkaKeysKlient(kafkaKeyConfig) {
        azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
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

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: ${throwable.message}", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    val server = embeddedServer(
        factory = Netty,
        port = 8080,
        configure = {
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
        }
    ) {
        module(
            registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            authProviders = applicationConfig.authProviders,
            kafkaKeyClient = kafkaKeyClient
        )
    }
    server.addShutdownHook {
        server.stop(300, 300)
    }
    server.start(wait = true)
}

fun Application.module(
    registry: PrometheusMeterRegistry,
    authProviders: AuthProviders,
    kafkaKeyClient: KafkaKeysClient
) {
    configureMetrics(registry)
    configureHTTP()
    configureAuthentication(authProviders)
    configureLogging()
    configureSerialization()
    configureOtel()

    routing {
        healthRoutes(registry)
        swaggerRoutes()
        rapporteringRoutes(kafkaKeyClient)
    }
}

