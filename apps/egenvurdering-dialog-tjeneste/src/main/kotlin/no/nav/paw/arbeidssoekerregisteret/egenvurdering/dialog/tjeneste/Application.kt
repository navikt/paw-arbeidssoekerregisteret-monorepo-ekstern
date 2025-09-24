package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureDatabase
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.config.env.appNameOrDefaultForLocal
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
    configureSerialization()
    configureHTTP()
    configureLogging()
    configureMetrics(applicationContext)
    configureDatabase(applicationContext.dataSource)
    configureKafka(applicationContext) { records: ConsumerRecords<Long, Egenvurdering> ->
        if (!records.isEmpty) {
            applicationContext.dialogService.varsleVeilederOmEgenvurderingAvProfilering(records)
        }
    }
    configureRouting(applicationContext)
}
