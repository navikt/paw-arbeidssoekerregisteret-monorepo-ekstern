package no.nav.paw.arbeidssoekerregisteret.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

fun Application.configureKafka(applicationContext: ApplicationContext, recordHandler: ((ConsumerRecords<Long, SpecificRecord>) -> Unit)) {
    with(applicationContext) {
        install(KafkaProducerPlugin) {
            kafkaProducers = listOf(producer)
        }
        install(KafkaConsumerPlugin<Long, SpecificRecord>("ProfileringOgPeriode")) {
            kafkaConsumer = applicationContext.consumer
            kafkaTopics = emptyList() //listOf(topics.profileringTopic, topics.periodeTopic)
            consumeFunction = recordHandler
        }
    }
}
