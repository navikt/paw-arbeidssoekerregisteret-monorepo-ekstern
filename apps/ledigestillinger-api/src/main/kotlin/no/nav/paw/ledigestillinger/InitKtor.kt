package no.nav.paw.ledigestillinger

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.database.plugin.DataSourcePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.LivenessCheck
import no.nav.paw.health.ReadinessCheck
import no.nav.paw.health.StartupCheck
import no.nav.paw.health.livenessRoute
import no.nav.paw.health.readinessRoute
import no.nav.paw.health.startupRoute
import no.nav.paw.hwm.DataConsumer
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.context.ApplicationContext
import no.nav.paw.ledigestillinger.plugin.CleanAwareFlywayPlugin
import no.nav.paw.ledigestillinger.plugin.installKafkaConsumerPlugin
import no.nav.paw.ledigestillinger.plugin.installLogginPlugin
import no.nav.paw.ledigestillinger.plugin.installWebPlugins
import no.nav.paw.ledigestillinger.route.stillingRoutes
import no.nav.paw.ledigestillinger.serde.AdAvroDeserializer
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.*
import javax.sql.DataSource

fun <A> initEmbeddedKtorServer(
    applicationContext: ApplicationContext
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> where
        A : LivenessCheck, A : ReadinessCheck, A : StartupCheck {
    with(applicationContext) {
        val pamStillingerKafkaConsumer = createHwmKafkaConsumer(
            config = applicationContext.applicationConfig.pamStillingerKafkaConsumer,
            keyDeserializer = UUIDDeserializer::class,
            valueDeserializer = AdAvroDeserializer::class,
            consumeFunction = stillingService::handleMessages
        )
        val healthChecks = createHealthChecks()
        return embeddedServer(Netty, port = 8080) {
            configureKtorServer(
                securityConfig = securityConfig,
                meterRegistry = meterRegistry,
                meterBinders = meterBinderList,
                dataSource = dataSource,
                pamStillingerKafkaConsumer = pamStillingerKafkaConsumer
            )
            routing {
                livenessRoute(healthChecks)
                readinessRoute(healthChecks)
                startupRoute(healthChecks)
                metricsRoutes(meterRegistry)
                swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
                stillingRoutes(stillingService)
            }
        }
    }
}

fun Application.configureKtorServer(
    securityConfig: SecurityConfig,
    meterRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    dataSource: DataSource,
    pamStillingerKafkaConsumer: DataConsumer<Message<UUID, Ad>, UUID, Ad>,
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
    install(DataSourcePlugin) {
        this.dataSource = dataSource
    }
    install(CleanAwareFlywayPlugin) {
        this.dataSource = dataSource
        this.cleanBeforeMigrate = false
    }
    installKafkaConsumerPlugin(pamStillingerKafkaConsumer)
}

