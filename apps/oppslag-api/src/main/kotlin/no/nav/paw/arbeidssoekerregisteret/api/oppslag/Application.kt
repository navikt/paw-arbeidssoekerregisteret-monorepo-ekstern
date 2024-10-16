package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.opplysningerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.perioderRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.profileringRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.samletInformasjonRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.migrateDatabase
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    // Konfigurasjon
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

    // Avhengigheter
    val dependencies = createDependencies(applicationConfig, kafkaConfig)

    // Clean database pga versjon oppdatering
    // cleanDatabase(dependencies.dataSource)

    // Kjør migration på database
    migrateDatabase(dependencies.dataSource)

    // Konsumer periode meldinger fra Kafka
    val threadPoolExecutor = ThreadPoolExecutor(4, 8, 1, TimeUnit.MINUTES, LinkedBlockingQueue())
    runAsync({
        try {
            dependencies.periodeKafkaConsumer.subscribe()
            dependencies.opplysningerKafkaConsumer.subscribe()
            dependencies.profileringKafkaConsumer.subscribe()
            while (true) {
                consume(dependencies, applicationConfig)
            }
        } catch (e: Exception) {
            logger.error("Consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }, threadPoolExecutor)

    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        dependencies.scheduleGetAktivePerioderGaugeService.scheduleGetAktivePerioderTask()
    }

    val server =
        embeddedServer(
            factory = Netty,
            configure = {
                callGroupSize = 8
                workerGroupSize = 8
                connectionGroupSize = 8
            },
            port = 8080,
            host = "0.0.0.0",
            module = { module(dependencies, applicationConfig) }
        )
            .start(wait = true)

    server.addShutdownHook {
        server.stop(300, 300)
        dependencies.profileringKafkaConsumer.stop()
        dependencies.opplysningerKafkaConsumer.stop()
        dependencies.periodeKafkaConsumer.stop()
    }
}

fun Application.module(
    dependencies: Dependencies,
    config: ApplicationConfig
) {
    // Konfigurerer plugins
    configureMetrics(
        dependencies.registry,
        dependencies.profileringKafkaConsumer.consumer,
        dependencies.periodeKafkaConsumer.consumer,
        dependencies.opplysningerKafkaConsumer.consumer
    )
    configureHTTP()
    configureAuthentication(config.authProviders)
    configureLogging()
    configureSerialization()

    // Ruter
    routing {
        healthRoutes(dependencies.registry)
        swaggerRoutes()
        perioderRoutes(
            dependencies.authorizationService,
            dependencies.periodeService
        )
        opplysningerRoutes(
            dependencies.authorizationService,
            dependencies.periodeService,
            dependencies.opplysningerService
        )
        profileringRoutes(
            dependencies.authorizationService,
            dependencies.periodeService,
            dependencies.profileringService
        )
        samletInformasjonRoutes(
            dependencies.authorizationService,
            dependencies.periodeService,
            dependencies.opplysningerService,
            dependencies.profileringService
        )
    }
}

@WithSpan(
    value = "consume",
    kind = SpanKind.INTERNAL
)
fun consume(
    dependencies: Dependencies,
    config: ApplicationConfig
) {
    dependencies.periodeKafkaConsumer.getAndProcessBatch(config.periodeTopic)
    dependencies.opplysningerKafkaConsumer.getAndProcessBatch(config.opplysningerOmArbeidssoekerTopic)
    dependencies.profileringKafkaConsumer.getAndProcessBatch(config.profileringTopic)
}
