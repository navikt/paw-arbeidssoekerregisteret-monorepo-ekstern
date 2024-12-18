package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
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
    bekreftelseKafkaConsumer: KafkaConsumer<Long, Bekreftelse>,
    periodeService: PeriodeService,
    opplysningerService: OpplysningerService,
    profileringService: ProfileringService,
    bekreftelseService: BekreftelseService
) {
    install(KafkaConsumerPlugin<Long, Periode>("Perioder")) {
        kafkaConsumer = periodeKafkaConsumer
        kafkaTopics = listOf(applicationConfig.perioderTopic)
        consumeFunction = periodeService::handleRecords
    }
    install(KafkaConsumerPlugin<Long, OpplysningerOmArbeidssoeker>("Opplysninger")) {
        kafkaConsumer = opplysningerKafkaConsumer
        kafkaTopics = listOf(applicationConfig.opplysningerTopic)
        consumeFunction = opplysningerService::handleRecords
    }
    install(KafkaConsumerPlugin<Long, Profilering>("Profileringer")) {
        kafkaConsumer = profileringKafkaConsumer
        kafkaTopics = listOf(applicationConfig.profileringTopic)
        consumeFunction = profileringService::handleRecords
    }
    install(KafkaConsumerPlugin<Long, Bekreftelse>("Bekreftelser")) {
        kafkaConsumer = bekreftelseKafkaConsumer
        kafkaTopics = listOf(applicationConfig.bekreftelseTopic)
        consumeFunction = bekreftelseService::handleRecords
    }
}