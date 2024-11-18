package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.kafkaConsumerThreadPoolExecutor
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.migrateDatabase
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import kotlin.concurrent.thread

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

    val applicationContext = ApplicationContext.build(applicationConfig, kafkaConfig)

    // cleanDatabase(dependencies.dataSource)
    migrateDatabase(applicationContext.dataSource)

    // Konsumer meldinger fra Kafka
    kafkaConsumerThreadPoolExecutor(applicationContext, applicationConfig)

    // Oppdaterer grafana gauge for antall aktive perioder
    thread {
        applicationContext.scheduleGetAktivePerioderGaugeService.scheduleGetAktivePerioderTask()
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
            module = { module(applicationContext, applicationConfig) }
        )
            .start(wait = true)

    server.addShutdownHook {
        server.stop(300, 300)
        applicationContext.profileringKafkaConsumer.stop()
        applicationContext.opplysningerKafkaConsumer.stop()
        applicationContext.periodeKafkaConsumer.stop()
    }
}

fun Application.module(
    applicationContext: ApplicationContext,
    config: ApplicationConfig
) {
    // Konfigurerer plugins
    configureMetrics(
        applicationContext.registry,
        applicationContext.profileringKafkaConsumer.consumer,
        applicationContext.periodeKafkaConsumer.consumer,
        applicationContext.opplysningerKafkaConsumer.consumer
    )
    configureHTTP()
    configureAuthentication(config.authProviders)
    configureLogging()
    configureSerialization()

    // Ruter
    routing {
        healthRoutes(applicationContext.registry)
        swaggerRoutes()
        perioderRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService
        )
        opplysningerRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService,
            applicationContext.opplysningerService
        )
        profileringRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService,
            applicationContext.profileringService
        )
        samletInformasjonRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService,
            applicationContext.opplysningerService,
            applicationContext.profileringService,
            applicationContext.bekreftelseService
        )
        bekreftelseRoutes(
            applicationContext.authorizationService,
            applicationContext.bekreftelseService,
            applicationContext.periodeService
        )
    }
}
