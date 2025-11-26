package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.context.consumerVersion
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.installTracingPlugin
import no.nav.paw.arbeidssoekerregisteret.plugins.installWebPlugins
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.utils.configureJacksonOverrides
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugins
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.hwm.updateHwm
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
    installContentNegotiationPlugin {
        configureJacksonOverrides()
    }
    installWebPlugins()
    installLoggingPlugin()
    installTracingPlugin()
    installMetricsPlugin(applicationContext.prometheusMeterRegistry)
    installErrorHandlingPlugin()
    installDatabasePlugins(applicationContext.datasource)
    installAuthenticationPlugin(applicationContext.securityConfig.authProviders)
    configureRouting(applicationContext)
    configureKafka(applicationContext) { records ->
        if (!records.isEmpty) {
            transaction {
                records.asSequence().lagrePerioderOgProfileringer()
            }
        }
    }
}

fun Sequence<ConsumerRecord<Long, SpecificRecord>>.lagrePerioderOgProfileringer(
    repo: EgenvurderingRepository = EgenvurderingPostgresRepository,
) = filter { record ->
    updateHwm(
        consumerVersion = consumerVersion,
        topic = record.topic(),
        partition = record.partition(),
        offset = record.offset()
    )
}.forEach { record ->
    when (val value = record.value()) {
        is Periode -> {
            if (value.avsluttet != null) repo.slettPeriode(value.id)
            else repo.lagrePeriode(value)
        }

        is Profilering -> repo.lagreProfilering(value)
    }
}
