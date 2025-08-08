package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureScheduledTask
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.build()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext.serverConfig) {
        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(
            factory = Netty,
            host = host,
            port = port
        ) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    with(applicationContext) {
        configureHTTP()
        configureLogging()
        configureSerialization()
        configureMetrics(
            prometheusMeterRegistry,
            periodeKafkaConsumer,
            opplysningerKafkaConsumer,
            profileringKafkaConsumer,
            egenvurderingKafkaConsumer,
            bekreftelseKafkaConsumer
        )
        configureAuthentication(securityConfig)
        configureDatabase(dataSource)
        configureScheduledTask(applicationConfig, metricsService)
        configureKafka(
            applicationConfig = applicationConfig,
            periodeKafkaConsumer = periodeKafkaConsumer,
            periodeConsumerLivenessProbe = periodeKafkaConsumerLivenessProbe,
            opplysningerKafkaConsumer = opplysningerKafkaConsumer,
            opplysningerKafkaConsumerLivenessProbe = opplysningerKafkaConsumerLivenessProbe,
            profileringKafkaConsumer = profileringKafkaConsumer,
            profileringKafkaConsumerLivenessProbe = profileringKafkaConsumerLivenessProbe,
            egenvurderingKafkaConsumer = egenvurderingKafkaConsumer,
            egenvurderingKafkaConsumerLivenessProbe = egenvurderingKafkaConsumerLivenessProbe,
            bekreftelseKafkaConsumer = bekreftelseKafkaConsumer,
            bekreftelseKafkaConsumerLivenessProbe = bekreftelseKafkaConsumerLivenessProbe,
            kafkaConsumerHandler = kafkaConsumerHandler,
            periodeService = periodeService,
            opplysningerService = opplysningerService,
            profileringService = profileringService,
            egenvurderingService = egenvurderingService,
            bekreftelseService = bekreftelseService
        )
        configureRouting(applicationContext)
    }
}
