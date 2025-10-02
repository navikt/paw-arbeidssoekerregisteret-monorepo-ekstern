package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.KafkaConsumerHandler
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.health.probes.GenericLivenessProbe
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.configureKafka(
    applicationConfig: ApplicationConfig,
    periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    periodeConsumerLivenessProbe: GenericLivenessProbe,
    opplysningerKafkaConsumer: KafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    opplysningerKafkaConsumerLivenessProbe: GenericLivenessProbe,
    profileringKafkaConsumer: KafkaConsumer<Long, Profilering>,
    profileringKafkaConsumerLivenessProbe: GenericLivenessProbe,
    bekreftelseKafkaConsumer: KafkaConsumer<Long, Bekreftelse>,
    bekreftelseKafkaConsumerLivenessProbe: GenericLivenessProbe,
    kafkaConsumerHandler: KafkaConsumerHandler,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService,
    bekreftelseService: BekreftelseService,
) {
    install(KafkaConsumerPlugin<Long, Periode>("Perioder")) {
        kafkaConsumer = periodeKafkaConsumer
        kafkaTopics = listOf(applicationConfig.perioderTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                periodeService.handleRecords(records)
            }
        }
        successFunction = {
            if (!it.isEmpty) periodeKafkaConsumer.commitSync()
            periodeConsumerLivenessProbe.markAlive()
        }
        errorFunction = { throwable: Throwable ->
            periodeConsumerLivenessProbe.markUnhealthy()
            kafkaConsumerHandler.handleException(throwable)
        }
    }
    install(KafkaConsumerPlugin<Long, OpplysningerOmArbeidssoeker>("Opplysninger")) {
        kafkaConsumer = opplysningerKafkaConsumer
        kafkaTopics = listOf(applicationConfig.opplysningerTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                opplysningerService.handleRecords(records)
            }
        }
        successFunction = {
            if (!it.isEmpty) opplysningerKafkaConsumer.commitSync()
            opplysningerKafkaConsumerLivenessProbe.markAlive()
        }
        errorFunction = { throwable: Throwable ->
            opplysningerKafkaConsumerLivenessProbe.markUnhealthy()
            kafkaConsumerHandler.handleException(throwable)
        }

    }
    install(KafkaConsumerPlugin<Long, Profilering>("Profileringer")) {
        kafkaConsumer = profileringKafkaConsumer
        kafkaTopics = listOf(applicationConfig.profileringTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                profileringService.handleRecords(records)
            }
        }
        successFunction = {
            if (!it.isEmpty) profileringKafkaConsumer.commitSync()
            profileringKafkaConsumerLivenessProbe.markAlive()
        }
        errorFunction = { throwable: Throwable ->
            profileringKafkaConsumerLivenessProbe.markUnhealthy()
            kafkaConsumerHandler.handleException(throwable)
        }
    }
    install(KafkaConsumerPlugin<Long, Bekreftelse>("Bekreftelser")) {
        kafkaConsumer = bekreftelseKafkaConsumer
        kafkaTopics = listOf(applicationConfig.bekreftelseTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                bekreftelseService.handleRecords(records)
            }
        }
        successFunction = {
            if (!it.isEmpty) bekreftelseKafkaConsumer.commitSync()
            bekreftelseKafkaConsumerLivenessProbe.markAlive()
        }
        errorFunction = { throwable: Throwable ->
            bekreftelseKafkaConsumerLivenessProbe.markUnhealthy()
            kafkaConsumerHandler.handleException(throwable)
        }
    }
}
