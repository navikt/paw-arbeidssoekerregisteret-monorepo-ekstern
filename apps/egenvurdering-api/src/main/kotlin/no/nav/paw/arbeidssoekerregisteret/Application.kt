package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.hwm.updateHwm
import no.nav.paw.arbeidssoekerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssoekerregisteret.plugins.configureDatabase
import no.nav.paw.arbeidssoekerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.config.env.appNameOrDefaultForLocal
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
    configureSerialization()
    configureHTTP()
    configureLogging()
    configureMetrics(applicationContext)
    configureDatabase(applicationContext.datasource)
    configureAuthentication(applicationContext)
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
) {
    filter { record ->
        updateHwm(
            consumerVersion = 1,
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
}
