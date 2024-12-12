package no.nav.paw.arbeidssoekerregisteret.api.oppslag.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.DATABASE_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.BatchKafkaConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PoaoTilgangHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.MetricsService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.BekreftelseDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.OpplysningerOmArbeidssoekerDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ProfileringDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.createPoaoTilgangClient
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.generateDatasource
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.pdl.factory.createPdlClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val kafkaConfig: KafkaConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val metricsService: MetricsService,
    val authorizationService: AuthorizationService,
    val periodeService: PeriodeService,
    val opplysningerService: OpplysningerService,
    val profileringService: ProfileringService,
    val bekreftelseService: BekreftelseService,
    val periodeKafkaConsumer: BatchKafkaConsumer<Long, Periode>,
    val opplysningerKafkaConsumer: BatchKafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    val profileringKafkaConsumer: BatchKafkaConsumer<Long, Profilering>,
    val bekreftelseKafkaConsumer: BatchKafkaConsumer<Long, Bekreftelse>
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val dataSource = generateDatasource(databaseConfig)
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val periodeRepository = PeriodeRepository()
            val opplysningerRepository = OpplysningerRepository()
            val profileringRepository = ProfileringRepository()
            val bekreftelseRepository = BekreftelseRepository()

            val pdlClient = createPdlClient()
            val poaoTilgangClient = createPoaoTilgangClient()

            val authorizationService = AuthorizationService(
                serverConfig = serverConfig,
                pdlHttpConsumer = PdlHttpConsumer(pdlClient),
                poaoTilgangHttpConsumer = PoaoTilgangHttpConsumer(poaoTilgangClient)
            )

            val kafkaFactory = KafkaFactory(kafkaConfig)

            val metricsService = MetricsService(prometheusMeterRegistry, periodeRepository)
            val periodeService =
                PeriodeService(periodeRepository, opplysningerRepository, profileringRepository, bekreftelseRepository)
            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.perioderGroupId,
                clientId = "${applicationConfig.perioderGroupId}-perioder-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class
            )
            val periodeBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.perioderTopic,
                periodeKafkaConsumer,
                periodeService::lagreAllePerioder
            )

            // Opplysninger avhengigheter

            val opplysningerService = OpplysningerService(opplysningerRepository)
            val opplysningerKafkaConsumer = kafkaFactory.createConsumer<Long, OpplysningerOmArbeidssoeker>(
                groupId = applicationConfig.opplysningerGroupId,
                clientId = "${applicationConfig.opplysningerGroupId}-opplysninger-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = OpplysningerOmArbeidssoekerDeserializer::class
            )
            val opplysningerBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.opplysningerTopic,
                opplysningerKafkaConsumer,
                opplysningerService::lagreAlleOpplysninger
            )

            // Profilering avhengigheter
            val profileringService = ProfileringService(profileringRepository)
            val profileringKafkaConsumer = kafkaFactory.createConsumer<Long, Profilering>(
                groupId = applicationConfig.profileringGroupId,
                clientId = "${applicationConfig.profileringGroupId}-profileringer-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = ProfileringDeserializer::class
            )
            val profileringBatchKafkaConsumer = BatchKafkaConsumer(
                applicationConfig.profileringTopic,
                profileringKafkaConsumer,
                profileringService::lagreAlleProfileringer
            )

            // Bekreftelse avhengigheter
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
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                kafkaConfig = kafkaConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                metricsService = metricsService,
                authorizationService = authorizationService,
                periodeService = periodeService,
                opplysningerService = opplysningerService,
                profileringService = profileringService,
                bekreftelseService = bekreftelseService,
                periodeKafkaConsumer = periodeBatchKafkaConsumer,
                opplysningerKafkaConsumer = opplysningerBatchKafkaConsumer,
                profileringKafkaConsumer = profileringBatchKafkaConsumer,
                bekreftelseKafkaConsumer = bekreftelseBatchKafkaConsumer
            )
        }
    }
}
