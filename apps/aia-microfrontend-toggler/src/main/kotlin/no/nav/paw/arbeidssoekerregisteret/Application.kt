package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APP_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.Config
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(APP_LOGGER_NAME)
    val config = loadNaisOrLocalConfiguration<Config>(CONFIG_FILE_NAME)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    logger.info("Starter ${config.app.id}")

    val server = embeddedServer(
        factory = Netty,
        port = config.server.port,
        configure = {
            callGroupSize = config.server.callGroupSize
            workerGroupSize = config.server.workerGroupSize
            connectionGroupSize = config.server.connectionGroupSize
        }
    ) {
        module(config, meterRegistry)
    }
    server.addShutdownHook {
        server.stop(config.server.gracePeriodMillis, config.server.timeoutMillis)
    }
    server.start(wait = true)

    logger.info("Avslutter ${config.app.id}")
}

fun Application.module(
    config: Config,
    meterRegistry: PrometheusMeterRegistry,
) {
    configureMetrics(meterRegistry)
    configureRouting(meterRegistry)
}