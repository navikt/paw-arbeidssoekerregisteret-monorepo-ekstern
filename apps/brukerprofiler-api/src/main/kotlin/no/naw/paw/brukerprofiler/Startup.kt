package no.naw.paw.brukerprofiler

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.DatasourceLivenessProbe
import no.nav.paw.hwm.HwmTopicConfig
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.asMessageConsumerWithHwmAndMetrics
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.naw.paw.brukerprofiler.db.initDatabase
import no.naw.paw.brukerprofiler.db.ops.lagreProfilering
import no.naw.paw.brukerprofiler.db.ops.opprettOgOppdaterBruker
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.LongDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

val appLogger = LoggerFactory.getLogger("brukerprofiler_api")

fun main() {
    appLogger.info("Starter brukerprofiler-api...")
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG)
    val topics = listOf(
        HwmTopicConfig(
            topic = topicNames.periodeTopic,
            consumerVersion = PERIODE_CONSUMER_VERSION
        ),
        HwmTopicConfig(
            topic = topicNames.profileringTopic,
            consumerVersion = PROFILERING_CONSUMER_VERSION
        )
    )
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val consumer = kafkaFactory.createConsumer<Long, SpecificRecord>(
        groupId = "brukerprofiler_api_consumer_v1",
        clientId = "brukerprofiler_api_v1_${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ActualSpecificAvroDeserializer::class
    ).asMessageConsumerWithHwmAndMetrics(
        prometheusMeterRegistry = prometheusMeterRegistry,
        receiver = ::process,
        hwmTopicConfig = topics
    )
    val dataSource = initDatabase(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val idClient = createKafkaKeyGeneratorClient()
    val appContext = ApplicationContext(
        consumer = consumer,
        dataSource = dataSource,
        prometheusMeterRegistry = prometheusMeterRegistry,
        securityConfig = securityConfig,
        healthChecks = healthChecksOf(
            //consumer, Tar ut denne slik at appen kjører selv om vi aldre startet å konsumere meldinger
            DatasourceLivenessProbe(dataSource)
        ),
        idClient = idClient
    )
    runApp(appContext)
}

fun process(source: Sequence<Message<Long, SpecificRecord>>) {
    source.forEach { message ->
        when (val value = message.value) {
            is Profilering -> lagreProfilering(value)
            is Periode -> opprettOgOppdaterBruker(value)
        }
    }
}

