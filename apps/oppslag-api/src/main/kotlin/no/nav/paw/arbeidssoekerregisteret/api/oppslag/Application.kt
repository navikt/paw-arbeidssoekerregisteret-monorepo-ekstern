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

    with(applicationContext) {
        logger.info("Starter $appName med hostname ${serverConfig.host} og port ${serverConfig.port}")

        embeddedServer(
            factory = Netty,
            host = serverConfig.host,
            port = serverConfig.port,
            configure = {
                callGroupSize = serverConfig.callGroupSize
                workerGroupSize = serverConfig.workerGroupSize
                connectionGroupSize = serverConfig.connectionGroupSize
            }) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(
                    gracePeriodMillis = serverConfig.gracePeriodMillis,
                    timeoutMillis = serverConfig.timeoutMillis
                )
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    configureHTTP()
    configureLogging()
    configureSerialization()
    configureMetrics(
        applicationContext.prometheusMeterRegistry,
        applicationContext.periodeKafkaConsumer,
        applicationContext.opplysningerKafkaConsumer,
        applicationContext.profileringKafkaConsumer,
        applicationContext.bekreftelseKafkaConsumer
    )
    configureAuthentication(applicationContext.securityConfig)
    configureDatabase(applicationContext.dataSource)
    configureScheduledTask(applicationContext.metricsService::tellAntallAktivePerioder)
    configureKafka(
        applicationContext.applicationConfig,
        applicationContext.periodeKafkaConsumer,
        applicationContext.opplysningerKafkaConsumer,
        applicationContext.profileringKafkaConsumer,
        applicationContext.bekreftelseKafkaConsumer,
        applicationContext.periodeService,
        applicationContext.opplysningerService,
        applicationContext.profileringService,
        applicationContext.bekreftelseService
    )
    configureRouting(applicationContext)
}
