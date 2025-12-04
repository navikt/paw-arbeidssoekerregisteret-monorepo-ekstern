package no.nav.paw.ledigestillinger

import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.database.plugin.installDatabasePlugins
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.hwm.DataConsumer
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.context.ApplicationContext
import no.nav.paw.ledigestillinger.context.TelemetryContext
import no.nav.paw.ledigestillinger.plugin.configureRouting
import no.nav.paw.ledigestillinger.plugin.installKafkaConsumerPlugin
import no.nav.paw.ledigestillinger.plugin.installLogginPlugin
import no.nav.paw.ledigestillinger.plugin.installWebPlugins
import no.nav.paw.ledigestillinger.serde.AdAvroDeserializer
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.*
import javax.sql.DataSource

fun initEmbeddedKtorServer(
    applicationContext: ApplicationContext
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    with(applicationContext) {
        val pamStillingerKafkaConsumer = createHwmKafkaConsumer(
            config = applicationContext.applicationConfig.pamStillingerKafkaConsumer,
            keyDeserializer = UUIDDeserializer::class,
            valueDeserializer = AdAvroDeserializer::class,
            consumeFunction = stillingService::handleMessages
        )
        return embeddedServer(Netty, port = 8080) {
            configureKtorServer(
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                meterRegistry = meterRegistry,
                meterBinders = meterBinderList,
                telemetryContext = telemetryContext,
                dataSource = dataSource,
                stillingService = stillingService,
                pamStillingerKafkaConsumer = pamStillingerKafkaConsumer
            )
            configureRouting(
                healthChecks = healthChecks,
                meterRegistry = meterRegistry,
                stillingService = stillingService
            )
        }
    }
}

fun Application.configureKtorServer(
    applicationConfig: ApplicationConfig,
    securityConfig: SecurityConfig,
    meterRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    telemetryContext: TelemetryContext,
    dataSource: DataSource,
    stillingService: StillingService,
    pamStillingerKafkaConsumer: DataConsumer<Message<UUID, Ad>, UUID, Ad>
) {
    installWebPlugins()
    installContentNegotiationPlugin()
    installLogginPlugin()
    installErrorHandlingPlugin()
    installWebAppMetricsPlugin(
        meterRegistry = meterRegistry,
        additionalMeterBinders = meterBinders
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
    /* TODO Midlertidig deaktivert
    installScheduledTaskPlugin(
        pluginInstance = applicationConfig.databaseCleanupScheduledTask.name,
        delay = applicationConfig.databaseCleanupScheduledTask.delay,
        period = applicationConfig.databaseCleanupScheduledTask.period,
        task = stillingService::slettUtloepteStillinger
    )
    */
}

