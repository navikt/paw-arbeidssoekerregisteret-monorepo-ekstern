package no.nav.paw.arbeidssoekerregisteret.eksternt.api

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfiguration
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.PeriodeConsumer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.kafka.serdes.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.metrics.AktivePerioderGaugeScheduler
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ScheduleDeletionService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

fun createDependencies(): Dependencies {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
    val dataSource = generateDatasource(applicationConfig.database.url)
    val database = Database.connect(dataSource)

    val periodeRepository = ArbeidssoekerperiodeRepository(database)
    val arbeidssoekerService = ArbeidssoekerService(periodeRepository)
    val scheduleDeletionService = ScheduleDeletionService(periodeRepository)
    val aktivePerioderGaugeScheduler = AktivePerioderGaugeScheduler(registry, periodeRepository)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    val consumer =
        kafkaFactory.createConsumer<Long, Periode>(
            groupId = applicationConfig.gruppeId,
            clientId = applicationConfig.gruppeId,
            keyDeserializer = LongDeserializer::class,
            valueDeserializer = PeriodeDeserializer::class
        )

    val periodeConsumer = PeriodeConsumer(applicationConfig.periodeTopic, consumer, arbeidssoekerService)

    return Dependencies(
        registry,
        arbeidssoekerService,
        periodeConsumer,
        dataSource,
        scheduleDeletionService,
        aktivePerioderGaugeScheduler,
        consumer
    )
}

data class Dependencies(
    val registry: PrometheusMeterRegistry,
    val arbeidssoekerService: ArbeidssoekerService,
    val periodeConsumer: PeriodeConsumer,
    val dataSource: DataSource,
    val scheduleDeletionService: ScheduleDeletionService,
    val aktivePerioderGaugeScheduler: AktivePerioderGaugeScheduler,
    val consumer: KafkaConsumer<Long, Periode>
)
