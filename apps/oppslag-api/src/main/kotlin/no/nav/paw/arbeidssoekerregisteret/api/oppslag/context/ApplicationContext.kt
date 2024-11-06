package no.nav.paw.arbeidssoekerregisteret.api.oppslag.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.BatchKafkaConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.TokenService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.BekreftelseDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.OpplysningerOmArbeidssoekerDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ProfileringDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.RetryInterceptor
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ScheduleGetAktivePerioderGaugeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.pdl.factory.createPdlClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import okhttp3.OkHttpClient
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import javax.sql.DataSource

data class ApplicationContext(
    val registry: PrometheusMeterRegistry,
    val dataSource: DataSource,
    val authorizationService: AuthorizationService,
    val periodeService: PeriodeService,
    val opplysningerService: OpplysningerService,
    val profileringService: ProfileringService,
    val bekreftelseService: BekreftelseService,
    val periodeKafkaConsumer: BatchKafkaConsumer<Long, Periode>,
    val opplysningerKafkaConsumer: BatchKafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    val profileringKafkaConsumer: BatchKafkaConsumer<Long, Profilering>,
    val bekreftelseKafkaConsumer: BatchKafkaConsumer<Long, Bekreftelse>,
    val scheduleGetAktivePerioderGaugeService: ScheduleGetAktivePerioderGaugeService
) {
    companion object {
        fun build(
            applicationConfig: ApplicationConfig,
            kafkaConfig: KafkaConfig
        ): ApplicationContext {
            val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val dataSource = generateDatasource(applicationConfig.database.url)

            val database = Database.connect(dataSource)

            val tokenService =
                applicationConfig.authProviders.find {
                    it.name == "azure"
                }?.run(::TokenService) ?: throw RuntimeException("Azure provider ikke funnet")

            val poaoTilgangHttpClient =
                PoaoTilgangCachedClient(
                    PoaoTilgangHttpClient(
                        applicationConfig.poaoClientConfig.url,
                        { tokenService.createMachineToMachineToken(applicationConfig.poaoClientConfig.scope) },
                        OkHttpClient.Builder().callTimeout(Duration.ofSeconds(6))
                            .addInterceptor(RetryInterceptor(maxRetries = 1)).build()
                    )
                )

            val pdlClient = createPdlClient()

            val authorizationService = AuthorizationService(PdlHttpConsumer(pdlClient), poaoTilgangHttpClient)

            val kafkaFactory = KafkaFactory(kafkaConfig)

            // Arbeidssøkerperiode avhengigheter
            val periodeRepository = PeriodeRepository(database)
            val scheduleGetAktivePerioderGaugeService =
                ScheduleGetAktivePerioderGaugeService(registry, periodeRepository)
            val periodeService = PeriodeService(periodeRepository)
            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.perioderGroupId,
                clientId = applicationConfig.perioderGroupId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class
            )
            val periodeBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.perioderTopic,
                periodeKafkaConsumer,
                periodeService::lagreAllePerioder
            )

            // Opplysninger avhengigheter
            val opplysningerRepository = OpplysningerRepository(database)
            val opplysningerService = OpplysningerService(opplysningerRepository)
            val opplysningerKafkaConsumer = kafkaFactory.createConsumer<Long, OpplysningerOmArbeidssoeker>(
                groupId = applicationConfig.opplysningerGroupId,
                clientId = applicationConfig.opplysningerGroupId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = OpplysningerOmArbeidssoekerDeserializer::class
            )
            val opplysningerBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.opplysningerTopic,
                opplysningerKafkaConsumer,
                opplysningerService::lagreAlleOpplysninger
            )

            // Profilering avhengigheter
            val profileringRepository = ProfileringRepository(database)
            val profileringService = ProfileringService(profileringRepository)
            val profileringKafkaConsumer = kafkaFactory.createConsumer<Long, Profilering>(
                groupId = applicationConfig.profileringGroupId,
                clientId = applicationConfig.profileringGroupId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = ProfileringDeserializer::class
            )
            val profileringBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.profileringTopic,
                profileringKafkaConsumer,
                profileringService::lagreAlleProfileringer
            )

            // Bekreftelse avhengigheter
            val bekreftelseRepository = BekreftelseRepository(database)
            val bekreftelseService = BekreftelseService(bekreftelseRepository)
            val bekreftelseKafkaConsumer = kafkaFactory.createConsumer<Long, Bekreftelse>(
                groupId = applicationConfig.bekreftelseGroupId,
                clientId = "${applicationConfig.bekreftelseGroupId}-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = BekreftelseDeserializer::class
            )
            val bekreftelseBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.bekreftelseTopic,
                bekreftelseKafkaConsumer,
                bekreftelseService::lagreAlleBekreftelser
            )

            return ApplicationContext(
                registry = registry,
                dataSource = dataSource,
                authorizationService = authorizationService,
                periodeService = periodeService,
                opplysningerService = opplysningerService,
                profileringService = profileringService,
                bekreftelseService = bekreftelseService,
                periodeKafkaConsumer = periodeBatchKafkaConsumer,
                opplysningerKafkaConsumer = opplysningerBatchKafkaConsumer,
                profileringKafkaConsumer = profileringBatchKafkaConsumer,
                bekreftelseKafkaConsumer = bekreftelseBatchKafkaConsumer,
                scheduleGetAktivePerioderGaugeService = scheduleGetAktivePerioderGaugeService
            )
        }
    }
}
