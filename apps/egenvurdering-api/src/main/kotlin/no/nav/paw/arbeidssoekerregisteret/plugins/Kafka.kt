package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.context.consumerVersion
import no.nav.paw.arbeidssoekerregisteret.context.partitionCount
import no.nav.paw.arbeidssoekerregisteret.hwm.initHwm
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureKafka(
    applicationContext: ApplicationContext,
    recordHandler: ((ConsumerRecords<Long, SpecificRecord>) -> Unit),
) = with(applicationContext) {
    transaction {
        initHwm(topics.profileringTopic, consumerVersion, partitionCount)
        initHwm(topics.periodeTopic, consumerVersion, partitionCount)
    }

    install(KafkaProducerPlugin) {
        kafkaProducers = listOf(producer)
    }
    install(KafkaConsumerPlugin<Long, SpecificRecord>("ProfileringOgPeriode")) {
        kafkaConsumer = consumer
        rebalanceListener = hwmRebalanceListener
        kafkaTopics = listOf(topics.profileringTopic, topics.periodeTopic)
        consumeFunction = recordHandler
        successFunction = { kafkaConsumerLivenessProbe.markAlive() }
        errorFunction = { throwable: Throwable ->
            kafkaConsumerLivenessProbe.markUnhealthy()
            throw throwable
        }
    }
}
