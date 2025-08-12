package no.nav.paw.arbeidssoekerregisteret.api.oppslag.context

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.KafkaConsumerHandler
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.BekreftelseService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.EgenvurderingService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.MetricsService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.BekreftelseDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.EgenvurderingDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.OpplysningerOmArbeidssoekerDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.PeriodeDeserializer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.ProfileringDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.probes.KafkaConsumerLivenessProbe
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.pdl.factory.createPdlClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.tilgangskontroll.client.TILGANGSKONTROLL_CLIENT_CONFIG
import no.nav.paw.tilgangskontroll.client.TilgangskontrollClientConfig
import no.nav.paw.tilgangskontroll.client.tilgangsTjenesteForAnsatte
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val kafkaConfig: KafkaConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val metricsService: MetricsService,
    val authorizationService: AuthorizationService,
    val periodeService: PeriodeService,
    val opplysningerService: OpplysningerService,
    val profileringService: ProfileringService,
    val bekreftelseService: BekreftelseService,
    val egenvurderingService: EgenvurderingService,
    val periodeKafkaConsumer: KafkaConsumer<Long, Periode>,
    val periodeKafkaConsumerLivenessProbe: KafkaConsumerLivenessProbe,
    val opplysningerKafkaConsumer: KafkaConsumer<Long, OpplysningerOmArbeidssoeker>,
    val opplysningerKafkaConsumerLivenessProbe: KafkaConsumerLivenessProbe,
    val profileringKafkaConsumer: KafkaConsumer<Long, Profilering>,
    val profileringKafkaConsumerLivenessProbe: KafkaConsumerLivenessProbe,
    val bekreftelseKafkaConsumer: KafkaConsumer<Long, Bekreftelse>,
    val bekreftelseKafkaConsumerLivenessProbe: KafkaConsumerLivenessProbe,
    val egenvurderingKafkaConsumer: KafkaConsumer<Long, Egenvurdering>,
    val egenvurderingKafkaConsumerLivenessProbe: KafkaConsumerLivenessProbe,
    val kafkaConsumerHandler: KafkaConsumerHandler,
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val azureM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
            val tilgangskontrollClientConfig = loadNaisOrLocalConfiguration<TilgangskontrollClientConfig>(
                TILGANGSKONTROLL_CLIENT_CONFIG
            )

            val dataSource = createHikariDataSource(databaseConfig)
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val periodeRepository = PeriodeRepository(prometheusMeterRegistry)
            val opplysningerRepository = OpplysningerRepository()
            val profileringRepository = ProfileringRepository()
            val bekreftelseRepository = BekreftelseRepository()
            val egenvurderingRepository = EgenvurderingRepository()

            val pdlClient = createPdlClient()

            val azureM2MTokenClient = createAzureAdM2MTokenClient(serverConfig.runtimeEnvironment, azureM2MConfig)

            val tilgangskontrollClient = tilgangsTjenesteForAnsatte(
                httpClient = HttpClient {
                    install(ContentNegotiation) {
                        jackson()
                    }
                },
                config = tilgangskontrollClientConfig,
                tokenProvider = { azureM2MTokenClient.createMachineToMachineToken(tilgangskontrollClientConfig.scope) }
            )

            val authorizationService = AuthorizationService(
                serverConfig = serverConfig,
                periodeRepository = periodeRepository,
                pdlHttpConsumer = PdlHttpConsumer(pdlClient),
                tilgangskontrollClient = tilgangskontrollClient
            )

            val kafkaFactory = KafkaFactory(kafkaConfig)

            val metricsService = MetricsService(prometheusMeterRegistry, periodeRepository)

            // Perioder avhengigheter
            val periodeService = PeriodeService(
                meterRegistry = prometheusMeterRegistry,
                periodeRepository = periodeRepository,
                opplysningerRepository = opplysningerRepository,
                profileringRepository = profileringRepository,
                egenvurderingRepository = egenvurderingRepository,
                bekreftelseRepository = bekreftelseRepository
            )
            val periodeKafkaConsumer = kafkaFactory.createConsumer<Long, Periode>(
                groupId = applicationConfig.perioderGroupId,
                clientId = "${applicationConfig.perioderGroupId}-perioder-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = PeriodeDeserializer::class,
                maxPollrecords = 10
            )
            val periodeKafkaConsumerLivenessProbe = KafkaConsumerLivenessProbe()

            // Opplysninger avhengigheter
            val opplysningerService = OpplysningerService(
                meterRegistry = prometheusMeterRegistry,
                opplysningerRepository = opplysningerRepository
            )
            val opplysningerKafkaConsumer = kafkaFactory.createConsumer<Long, OpplysningerOmArbeidssoeker>(
                groupId = applicationConfig.opplysningerGroupId,
                clientId = "${applicationConfig.opplysningerGroupId}-opplysninger-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = OpplysningerOmArbeidssoekerDeserializer::class
            )
            val opplysningsKafkaConsumerLivenessProbe = KafkaConsumerLivenessProbe()

            // Profileringer avhengigheter
            val profileringService = ProfileringService(
                meterRegistry = prometheusMeterRegistry,
                profileringRepository = profileringRepository
            )
            val profileringKafkaConsumer = kafkaFactory.createConsumer<Long, Profilering>(
                groupId = applicationConfig.profileringGroupId,
                clientId = "${applicationConfig.profileringGroupId}-profileringer-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = ProfileringDeserializer::class
            )
            val profileringKafkaConsumerLivenessProbe = KafkaConsumerLivenessProbe()

            // Bekreftelser avhengigheter
            val bekreftelseService = BekreftelseService(
                meterRegistry = prometheusMeterRegistry,
                bekreftelseRepository = bekreftelseRepository
            )
            val bekreftelseKafkaConsumer = kafkaFactory.createConsumer<Long, Bekreftelse>(
                groupId = applicationConfig.bekreftelseGroupId,
                clientId = "${applicationConfig.bekreftelseGroupId}-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = BekreftelseDeserializer::class
            )
            val bekreftelseKafkaConsumerLivenessProbe = KafkaConsumerLivenessProbe()

            // Egenvurderinger avhengigheter
            val egenvurderingService = EgenvurderingService(
                meterRegistry = prometheusMeterRegistry,
                egenvurderingRepository = egenvurderingRepository
            )

            val egenvurderingKafkaConsumer = kafkaFactory.createConsumer<Long, Egenvurdering>(
                groupId = applicationConfig.egenvurderingGroupId,
                clientId = "${applicationConfig.egenvurderingGroupId}-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = EgenvurderingDeserializer::class
            )
            val egenvurderingKafkaConsumerLivenessProbe = KafkaConsumerLivenessProbe()

            val kafkaConsumerHandler = KafkaConsumerHandler()


            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                kafkaConfig = kafkaConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                metricsService = metricsService,
                authorizationService = authorizationService,
                periodeService = periodeService,
                opplysningerService = opplysningerService,
                profileringService = profileringService,
                bekreftelseService = bekreftelseService,
                egenvurderingService = egenvurderingService,
                periodeKafkaConsumer = periodeKafkaConsumer,
                periodeKafkaConsumerLivenessProbe = periodeKafkaConsumerLivenessProbe,
                opplysningerKafkaConsumer = opplysningerKafkaConsumer,
                opplysningerKafkaConsumerLivenessProbe = opplysningsKafkaConsumerLivenessProbe,
                profileringKafkaConsumer = profileringKafkaConsumer,
                profileringKafkaConsumerLivenessProbe = profileringKafkaConsumerLivenessProbe,
                bekreftelseKafkaConsumer = bekreftelseKafkaConsumer,
                bekreftelseKafkaConsumerLivenessProbe = bekreftelseKafkaConsumerLivenessProbe,
                egenvurderingKafkaConsumer = egenvurderingKafkaConsumer,
                egenvurderingKafkaConsumerLivenessProbe = egenvurderingKafkaConsumerLivenessProbe,
                kafkaConsumerHandler = kafkaConsumerHandler
            )
        }
    }
}
