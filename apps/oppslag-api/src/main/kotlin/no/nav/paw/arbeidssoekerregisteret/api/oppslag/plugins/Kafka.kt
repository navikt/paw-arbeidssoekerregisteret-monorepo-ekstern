package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.KafkaConsumerHandler
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.configureKafka(
    applicationConfig: ApplicationConfig,
    periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    opplysningerKafkaConsumer: KafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    profileringKafkaConsumer: KafkaConsumer<Long, Profilering>,
    egenvurderingKafkaConsumer: KafkaConsumer<Long, Egenvurdering>,
    bekreftelseKafkaConsumer: KafkaConsumer<Long, Bekreftelse>,
    kafkaConsumerHandler: KafkaConsumerHandler,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService,
    egenvurderingService: EgenvurderingService,
    bekreftelseService: BekreftelseService
) {
    install(KafkaConsumerPlugin<Long, Periode>("Perioder")) {
        kafkaConsumer = periodeKafkaConsumer
        kafkaTopics = listOf(applicationConfig.perioderTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                periodeService.handleRecords(records)
            }
        }
        errorFunction = kafkaConsumerHandler::handleException
    }
    install(KafkaConsumerPlugin<Long, OpplysningerOmArbeidssoeker>("Opplysninger")) {
        kafkaConsumer = opplysningerKafkaConsumer
        kafkaTopics = listOf(applicationConfig.opplysningerTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                opplysningerService.handleRecords(records)
            }
        }
        errorFunction = kafkaConsumerHandler::handleException
    }
    install(KafkaConsumerPlugin<Long, Profilering>("Profileringer")) {
        kafkaConsumer = profileringKafkaConsumer
        kafkaTopics = listOf(applicationConfig.profileringTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                profileringService.handleRecords(records)
            }
        }
        errorFunction = kafkaConsumerHandler::handleException
    }
    install(KafkaConsumerPlugin<Long, Egenvurdering>("Egenvurderinger")) {
        kafkaConsumer = egenvurderingKafkaConsumer
        kafkaTopics = listOf(applicationConfig.egenvurderingTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                egenvurderingService.handleRecords(records)
            }
        }
        errorFunction = kafkaConsumerHandler::handleException
    }
    install(KafkaConsumerPlugin<Long, Bekreftelse>("Bekreftelser")) {
        kafkaConsumer = bekreftelseKafkaConsumer
        kafkaTopics = listOf(applicationConfig.bekreftelseTopic)
        consumeFunction = { records ->
            if (!records.isEmpty) {
                bekreftelseService.handleRecords(records)
            }
        }
        errorFunction = kafkaConsumerHandler::handleException
    }
}
