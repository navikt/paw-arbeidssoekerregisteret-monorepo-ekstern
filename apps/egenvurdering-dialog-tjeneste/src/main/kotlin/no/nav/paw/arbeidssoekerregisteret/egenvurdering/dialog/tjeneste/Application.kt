package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.installTracingPlugin
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.installWebPlugins
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.configureJacksonOverrides
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords

fun main() {
    val logger = buildApplicationLogger
    val applicationContext = ApplicationContext.create()

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
        installLoggingPlugin()
        installContentNegotiationPlugin {
            configureJacksonOverrides()
        }
        installTracingPlugin()
        installMetricsPlugin(meterRegistry)
        installAuthenticationPlugin(securityConfig.authProviders)
        installDatabasePlugins(dataSource)
        configureRouting(healthChecks, meterRegistry, dialogService)
        configureKafka(applicationContext) { records: ConsumerRecords<Long, Egenvurdering> ->
            if (!records.isEmpty) {
                dialogService.varsleVeilederOmEgenvurderingAvProfilering(records)
            }
        }
    }
}
