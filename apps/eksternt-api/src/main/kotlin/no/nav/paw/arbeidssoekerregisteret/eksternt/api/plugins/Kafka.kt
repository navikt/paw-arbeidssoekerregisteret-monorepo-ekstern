package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.configureKafka(
    applicationConfig: ApplicationConfig,
    periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    periodeService: PeriodeService
) {
    install(KafkaConsumerPlugin<Long, Periode>("Perioder")) {
        kafkaConsumer = periodeKafkaConsumer
        kafkaTopics = listOf(applicationConfig.periodeTopic)
        consumeFunction = periodeService::handleRecords
    }
}