package no.nav.paw.ledigestillinger.context

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.HealthChecks
import no.nav.paw.health.healthChecksOf
import no.nav.paw.hwm.HwmTopicConfig
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.asMessageConsumerWithHwmAndMetrics
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.config.KafkaConsumerConfig
import no.nav.paw.ledigestillinger.consumer.HwmMessageConsumer
import no.nav.paw.ledigestillinger.serde.AdAvroDeserializer
import no.nav.paw.ledigestillinger.service.StillingService
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.time.Clock
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass

data class ApplicationContext(
    val clock: Clock = Clock.systemDefaultZone(),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val databaseConfig: DatabaseConfig = loadNaisOrLocalConfiguration(DATABASE_CONFIG),
    val dataSource: DataSource = createHikariDataSource(databaseConfig),
    val meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    val meterBinderList: List<MeterBinder> = emptyList(),
    val telemetryContext: TelemetryContext = TelemetryContext(meterRegistry),
    val kafkaFactory: KafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG)),
    val stillingService: StillingService = StillingService(
        clock = clock,
        applicationConfig = applicationConfig,
        telemetryContext = telemetryContext
    ),
    val pamStillingerKafkaConsumer: HwmMessageConsumer<UUID, Ad> = kafkaFactory.createHwmKafkaConsumer(
        meterRegistry = meterRegistry,
        consumerConfig = applicationConfig.pamStillingerKafkaConsumer,
        keyDeserializer = UUIDDeserializer::class,
        valueDeserializer = AdAvroDeserializer::class,
        consumeFunction = stillingService::handleMessages
    )
) {
    val healthChecks get(): HealthChecks = healthChecksOf(pamStillingerKafkaConsumer)
}

private fun <K : Any, V : Any> KafkaFactory.createHwmKafkaConsumer(
    meterRegistry: PrometheusMeterRegistry,
    consumerConfig: KafkaConsumerConfig,
    keyDeserializer: KClass<out Deserializer<K>>,
    valueDeserializer: KClass<out Deserializer<V>>,
    consumeFunction: (Sequence<Message<K, V>>) -> Unit
): HwmMessageConsumer<K, V> = createConsumer(
    clientId = consumerConfig.clientId,
    groupId = consumerConfig.groupId,
    keyDeserializer = keyDeserializer,
    valueDeserializer = valueDeserializer
).asMessageConsumerWithHwmAndMetrics(
    prometheusMeterRegistry = meterRegistry,
    receiver = consumeFunction,
    hwmTopicConfig = listOf(
        HwmTopicConfig(
            topic = consumerConfig.topic,
            consumerVersion = consumerConfig.version
        )
    )
)