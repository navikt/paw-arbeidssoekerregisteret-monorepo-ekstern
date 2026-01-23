package no.nav.paw.ledigestillinger

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugins
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.ledigestillinger.context.ApplicationContext
import no.nav.paw.ledigestillinger.plugin.configureRouting
import no.nav.paw.ledigestillinger.plugin.installKafkaConsumerPlugin
import no.nav.paw.ledigestillinger.plugin.installLogginPlugin
import no.nav.paw.ledigestillinger.plugin.installWebPlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

fun main() {
    val logger = buildApplicationLogger
    val applicationContext = ApplicationContext()

    with(applicationContext.serverConfig) {
        val appName = runtimeEnvironment.appNameOrDefaultForLocal()

        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(
            factory = Netty,
            host = host,
            port = port
        ) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                stop(gracePeriodMillis, timeoutMillis)
                logger.info("Avslutter $appName")
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    with(applicationContext) {
        installWebPlugins()
        installContentNegotiationPlugin()
        installLogginPlugin()
        installErrorHandlingPlugin()
        installWebAppMetricsPlugin(
            meterRegistry = meterRegistry,
            additionalMeterBinders = meterBinderList
        )
        installAuthenticationPlugin(securityConfig.authProviders)
        installDatabasePlugins(dataSource)
        installKafkaConsumerPlugin(pamStillingerKafkaConsumer)
        installScheduledTaskPlugin(
            pluginInstance = applicationConfig.databaseMetricsScheduledTask.name,
            delay = applicationConfig.databaseMetricsScheduledTask.delay,
            period = applicationConfig.databaseMetricsScheduledTask.period,
            task = telemetryContext::tellLagredeStillinger
        )
        installScheduledTaskPlugin(
            pluginInstance = applicationConfig.databaseCleanupScheduledTask.name,
            delay = applicationConfig.databaseCleanupScheduledTask.delay,
            period = applicationConfig.databaseCleanupScheduledTask.period,
            task = stillingService::slettUtloepteStillinger
        )
        configureRouting(
            healthChecks = healthChecks,
            meterRegistry = meterRegistry,
            stillingService = stillingService
        )
    }
}
