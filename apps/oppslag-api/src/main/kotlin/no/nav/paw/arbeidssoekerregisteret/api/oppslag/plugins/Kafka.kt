package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

fun Application.configureKafka(
    applicationConfig: ApplicationConfig,
    periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    opplysningerKafkaConsumer: KafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    profileringKafkaConsumer: KafkaConsumer<Long, Profilering>,
    bekreftelseKafkaConsumer: KafkaConsumer<Long, Bekreftelse>,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService,
    bekreftelseService: BekreftelseService
) {
    val applicationShutdownHandler: (Application) -> Unit = { app ->
        val logger = buildApplicationLogger
        logger.error("Avslutter applikasjonen pga feil i consumer")

        app.monitor.raise(ApplicationStopping, app)

        Timer().schedule(5000) {
            logger.error("Tvungen avslutning av applikasjonen etter timeout")
            exitProcess(1)
        }
    }

    install(KafkaConsumerPlugin<Long, Periode>("Perioder")) {
        kafkaConsumer = periodeKafkaConsumer
        kafkaTopics = listOf(applicationConfig.perioderTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                periodeService.handleRecords(records)
            }
        }
        shutdownOnError = true
        shutdownHandler = applicationShutdownHandler
    }
    install(KafkaConsumerPlugin<Long, OpplysningerOmArbeidssoeker>("Opplysninger")) {
        kafkaConsumer = opplysningerKafkaConsumer
        kafkaTopics = listOf(applicationConfig.opplysningerTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                opplysningerService.handleRecords(records)
            }
        }
        shutdownOnError = true
        shutdownHandler = applicationShutdownHandler
    }
    install(KafkaConsumerPlugin<Long, Profilering>("Profileringer")) {
        kafkaConsumer = profileringKafkaConsumer
        kafkaTopics = listOf(applicationConfig.profileringTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                profileringService.handleRecords(records)
            }
        }
        shutdownOnError = true
        shutdownHandler = applicationShutdownHandler
    }
    install(KafkaConsumerPlugin<Long, Bekreftelse>("Bekreftelser")) {
        kafkaConsumer = bekreftelseKafkaConsumer
        kafkaTopics = listOf(applicationConfig.bekreftelseTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                bekreftelseService.handleRecords(records)
            }
        }
        shutdownOnError = true
        shutdownHandler = applicationShutdownHandler
    }
}
