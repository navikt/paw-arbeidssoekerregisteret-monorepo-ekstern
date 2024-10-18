package no.nav.paw.arbeidssoekerregisteret.api.oppslag.context

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.consumers.BatchConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.BekreftelseDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.OpplysningerOmArbeidssoekerDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.serdes.ProfileringDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.metrics.ScheduleGetAktivePerioderGaugeService
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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.RetryInterceptor
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.configureJackson
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.pdl.PdlClient
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
    val periodeKafkaConsumer: BatchConsumer<Long, Periode>,
    val opplysningerKafkaConsumer: BatchConsumer<Long, OpplysningerOmArbeidssoeker>,
    val profileringKafkaConsumer: BatchConsumer<Long, Profilering>,
    val bekreftelseKafkaConsumer: BatchConsumer<Long, Bekreftelse>,
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

            // OBO vs StS token
            val authorizationService = AuthorizationService(PdlHttpConsumer(pdlClient), poaoTilgangHttpClient)

            val kafkaFactory = KafkaFactory(kafkaConfig)

            // Arbeidssøkerperiode avhengigheter
            val periodeRepository = PeriodeRepository(database)
            val scheduleGetAktivePerioderGaugeService =
                ScheduleGetAktivePerioderGaugeService(registry, periodeRepository)
            val periodeService = PeriodeService(periodeRepository)
            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class
            )
            val periodeBatchKafkaConsumer =
                BatchConsumer(applicationConfig.periodeTopic, periodeKafkaConsumer, periodeService::lagreAllePerioder)

            // Situasjon avhengigheter
            val opplysningerRepository = OpplysningerRepository(database)
            val opplysningerService = OpplysningerService(opplysningerRepository)
            val opplysningerKafkaConsumer = kafkaFactory.createConsumer<Long, OpplysningerOmArbeidssoeker>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = OpplysningerOmArbeidssoekerDeserializer::class
            )
            val opplysningerBatchKafkaConsumer = BatchConsumer(
                applicationConfig.opplysningerOmArbeidssoekerTopic,
                opplysningerKafkaConsumer,
                opplysningerService::lagreAlleOpplysninger
            )

            // Profilering avhengigheter
            val profileringRepository = ProfileringRepository(database)
            val profileringService = ProfileringService(profileringRepository)
            val profileringKafkaConsumer = kafkaFactory.createConsumer<Long, Profilering>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = ProfileringDeserializer::class
            )
            val profileringBatchKafkaConsumer = BatchConsumer(
                applicationConfig.profileringTopic,
                profileringKafkaConsumer,
                profileringService::lagreAlleProfileringer
            )

            // Bekreftelse avhengigheter
            val bekreftelseRepository = BekreftelseRepository(database)
            val bekreftelseService = BekreftelseService(bekreftelseRepository)
            val bekreftelseKafkaConsumer = kafkaFactory.createConsumer<Long, Bekreftelse>(
                groupId = applicationConfig.gruppeId,
                clientId = applicationConfig.gruppeId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = BekreftelseDeserializer::class
            )
            val bekreftelseBatchKafkaConsumer = BatchConsumer(
                applicationConfig.bekreftelseTopic,
                bekreftelseKafkaConsumer,
                bekreftelseService::lagreBekreftelse
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
