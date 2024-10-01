package no.nav.paw.arbeidssoekerregisteret.api.oppslag

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.OpplysningerOmArbeidssoekerDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.ProfileringDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.metrics.ScheduleGetAktivePerioderGaugeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerOmArbeidssoekerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ArbeidssoekerperiodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AutorisasjonService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerOmArbeidssoekerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.TokenService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.RetryInterceptor
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import okhttp3.OkHttpClient
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import javax.sql.DataSource

fun createDependencies(
    config: ApplicationConfig,
    kafkaConfig: KafkaConfig
): Dependencies {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dataSource = generateDatasource(config.database.url)

    val database = Database.connect(dataSource)

    val tokenService =
        config.authProviders.find {
            it.name == "azure"
        }?.run(::TokenService) ?: throw RuntimeException("Azure provider ikke funnet")

    val poaoTilgangHttpClient =
        PoaoTilgangCachedClient(
            PoaoTilgangHttpClient(
                config.poaoClientConfig.url,
                { tokenService.createMachineToMachineToken(config.poaoClientConfig.scope) },
                OkHttpClient.Builder().callTimeout(Duration.ofSeconds(6)).addInterceptor(RetryInterceptor(maxRetries = 1)).build()
            )
        )

    // OBO vs StS token
    val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

    val kafkaFactory = KafkaFactory(kafkaConfig)

    // Arbeidssøkerperiode avhengigheter
    val arbeidssoekerperiodeRepository = ArbeidssoekerperiodeRepository(database)
    val scheduleGetAktivePerioderGaugeService = ScheduleGetAktivePerioderGaugeService(registry, arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeService = ArbeidssoekerperiodeService(arbeidssoekerperiodeRepository)
    val arbeidssoekerperiodeConsumer =
        kafkaFactory.createConsumer<Long, Periode>(
            groupId = config.gruppeId,
            clientId = config.gruppeId,
            keyDeserializer = LongDeserializer::class,
            valueDeserializer = PeriodeDeserializer::class
        )
    val arbeidssoekerperiodeBatchConsumer = BatchConsumer(config.periodeTopic, arbeidssoekerperiodeConsumer, arbeidssoekerperiodeService::lagreBatch)

    // Situasjon avhengigheter
    val opplysningerOmArbeidssoekerRepository = OpplysningerOmArbeidssoekerRepository(database)
    val opplysningerOmArbeidssoekerService = OpplysningerOmArbeidssoekerService(opplysningerOmArbeidssoekerRepository)
    val opplysningerOmArbeidssoekerConsumer =
        kafkaFactory.createConsumer<Long, OpplysningerOmArbeidssoeker>(
            groupId = config.gruppeId,
            clientId = config.gruppeId,
            keyDeserializer = LongDeserializer::class,
            valueDeserializer = OpplysningerOmArbeidssoekerDeserializer::class
        )
    val opplysningerOmArbeidssoekerBatchConsumer = BatchConsumer(config.opplysningerOmArbeidssoekerTopic, opplysningerOmArbeidssoekerConsumer, opplysningerOmArbeidssoekerService::lagreBatch)

    // Profilering avhengigheter
    val profileringRepository = ProfileringRepository(database)
    val profileringService = ProfileringService(profileringRepository)
    val profileringConsumer =
        kafkaFactory.createConsumer<Long, Profilering>(
            groupId = config.gruppeId,
            clientId = config.gruppeId,
            keyDeserializer = LongDeserializer::class,
            valueDeserializer = ProfileringDeserializer::class
        )
    val profileringBatchConsumer = BatchConsumer(config.profileringTopic, profileringConsumer, profileringService::lagreBatch)

    return Dependencies(
        registry,
        dataSource,
        arbeidssoekerperiodeService,
        arbeidssoekerperiodeBatchConsumer,
        opplysningerOmArbeidssoekerService,
        opplysningerOmArbeidssoekerBatchConsumer,
        profileringService,
        profileringBatchConsumer,
        autorisasjonService,
        scheduleGetAktivePerioderGaugeService
    )
}

data class Dependencies(
    val registry: PrometheusMeterRegistry,
    val dataSource: DataSource,
    val arbeidssoekerperiodeService: ArbeidssoekerperiodeService,
    val arbeidssoekerperiodeConsumer: BatchConsumer<Long, Periode>,
    val opplysningerOmArbeidssoekerService: OpplysningerOmArbeidssoekerService,
    val opplysningerOmArbeidssoekerConsumer: BatchConsumer<Long, OpplysningerOmArbeidssoeker>,
    val profileringService: ProfileringService,
    val profileringConsumer: BatchConsumer<Long, Profilering>,
    val autorisasjonService: AutorisasjonService,
    val scheduleGetAktivePerioderGaugeService: ScheduleGetAktivePerioderGaugeService
)
