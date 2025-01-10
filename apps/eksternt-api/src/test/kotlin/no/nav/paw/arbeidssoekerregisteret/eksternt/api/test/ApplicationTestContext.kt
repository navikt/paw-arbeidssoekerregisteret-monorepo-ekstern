package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ScheduledTaskService
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.configureJackson
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

class ApplicationTestContext private constructor(
    val dataSource: DataSource,
    val periodeRepository: PeriodeRepository
) {
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val mockOAuth2Server = MockOAuth2Server()
    val meterRegistryMock: PrometheusMeterRegistry = mockk(relaxed = true)
    val periodeKafkaConsumerMock: KafkaConsumer<Long, Periode> = mockk(relaxed = true)
    val periodeService = PeriodeService(periodeRepository)
    val scheduledTaskService = ScheduledTaskService(meterRegistryMock, periodeRepository)
    val applicationContext = ApplicationContext(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        securityConfig = securityConfig,
        dataSource = dataSource,
        meterRegistry = meterRegistryMock,
        periodeKafkaConsumer = periodeKafkaConsumerMock,
        periodeService = periodeService,
        scheduledTaskService = scheduledTaskService
    )

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }

    companion object {
        fun withMockDataAccess(): ApplicationTestContext {
            val dataSource: DataSource = mockk(relaxed = true)
            val periodeRepository: PeriodeRepository = mockk()
            return ApplicationTestContext(
                dataSource = dataSource,
                periodeRepository = periodeRepository
            )
        }

        fun withRealDataAccess(): ApplicationTestContext {
            val dataSource = initTestDatabase()
            val database = Database.connect(dataSource)
            val periodeRepository = PeriodeRepository(database)
            return ApplicationTestContext(
                dataSource = dataSource,
                periodeRepository = periodeRepository
            )
        }
    }
}