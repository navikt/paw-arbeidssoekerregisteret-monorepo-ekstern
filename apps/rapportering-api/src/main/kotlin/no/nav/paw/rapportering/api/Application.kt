package no.nav.paw.rapportering.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.rapportering.api.config.AuthProviders
import no.nav.paw.rapportering.api.config.CONFIG_FILE_NAME
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.plugins.configureAuthentication
import no.nav.paw.rapportering.api.plugins.configureHTTP
import no.nav.paw.rapportering.api.plugins.configureLogging
import no.nav.paw.rapportering.api.plugins.configureMetrics
import no.nav.paw.rapportering.api.plugins.configureOtel
import no.nav.paw.rapportering.api.plugins.configureSerialization
import no.nav.paw.rapportering.api.routes.healthRoutes
import no.nav.paw.rapportering.api.routes.swaggerRoutes
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter ${ApplicationInfo.id}")

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(CONFIG_FILE_NAME)

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
            authProviders = applicationConfig.authProviders
        )
    }
    server.addShutdownHook {
        server.stop(300, 300)
    }
    server.start(wait = true)
}

fun Application.module(
    registry: PrometheusMeterRegistry,
    authProviders: AuthProviders
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
    }
}

