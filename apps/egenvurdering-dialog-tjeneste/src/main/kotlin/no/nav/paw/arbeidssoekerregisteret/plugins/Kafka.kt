package no.nav.paw.arbeidssoekerregisteret.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaConsumerPlugin<Long, Egenvurdering>("egenvurdering-consumer")) {
            kafkaConsumer = consumer
            kafkaTopics = listOf(applicationConfig.kafkaTopology.egenvurderingTopic)
            consumeFunction = ::handleRecords
        }
    }
}

fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
    // TODO: send dialog med egenvurdering til veilarbdialog
}
