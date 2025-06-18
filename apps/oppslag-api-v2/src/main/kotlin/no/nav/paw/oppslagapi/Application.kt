package no.nav.paw.oppslagapi

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.oppslagapi.dataconsumer.DataConsumer
import no.nav.paw.oppslagapi.dataconsumer.kafka.HwmRebalanceListener
import no.nav.paw.oppslagapi.dataconsumer.kafka.hwm.initHwm
import no.nav.paw.oppslagapi.health.CompoudHealthIndicator
import no.nav.paw.oppslagapi.health.ExposedHealthIndicator
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

const val consumer_version = 1
const val consumer_group = "oppslag-api-v2-consumer-v1"
const val partition_count = 6

val appLogger = LoggerFactory.getLogger("app")

fun main() {
    appLogger.info("Starter oppslag-api-v2")
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dataSource = createHikariDataSource(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    Database.connect(dataSource)
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("db/migration")
        .cleanDisabled(false)
        .load()
        .migrate()
    transaction {
        topicNames.asList().forEach { topic ->
            initHwm(topic, consumer_version, partition_count)
        }
    }

    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG))
    val consumer: Consumer<Long, ByteArray> = kafkaFactory.createConsumer(
        groupId = consumer_group,
        clientId = "oppslag-api-v2-${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    val rebalanceListener = HwmRebalanceListener(consumer_version, consumer)
    consumer.subscribe(topicNames.asList(), rebalanceListener)
    val deserializer: Deserializer<SpecificRecord> = kafkaFactory.kafkaAvroDeSerializer()
    val consumerMetrics = KafkaClientMetrics(consumer)
    val dataConsumerTask = DataConsumer(
        deserializer = deserializer,
        consumer = consumer,
        pollTimeout = Duration.ofMillis(1000L)
    )
    dataConsumerTask.run()
    val healthIndicator = CompoudHealthIndicator(ExposedHealthIndicator, dataConsumerTask)
    initKtor(
        meterBinders = listOf(consumerMetrics),
        prometheusRegistry = prometheusRegistry,
        healthIndicator = healthIndicator
    ).start(wait = true)
}
