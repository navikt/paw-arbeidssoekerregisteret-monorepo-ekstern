package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ServerConfig
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
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.jetbrains.exposed.sql.Database

class ApplicationTestContext(
    val periodeRepository: PeriodeRepository,
    val opplysningerRepository: OpplysningerRepository,
    val profileringRepository: ProfileringRepository,
    val bekreftelseRepository: BekreftelseRepository
) {
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val mockOAuth2Server = MockOAuth2Server()
    val pdlHttpConsumerMock: PdlHttpConsumer = mockk<PdlHttpConsumer>()
    val tilgangskontrollClientMock: TilgangsTjenesteForAnsatte = mockk<TilgangsTjenesteForAnsatte>()
    val authorizationService: AuthorizationService = AuthorizationService(
        serverConfig = serverConfig,
        periodeRepository = periodeRepository,
        pdlHttpConsumer = pdlHttpConsumerMock,
        tilgangskontrollClient = tilgangskontrollClientMock
    )
    val periodeService = PeriodeService(
        periodeRepository = periodeRepository,
        opplysningerRepository = opplysningerRepository,
        profileringRepository = profileringRepository,
        bekreftelseRepository = bekreftelseRepository
    )
    val opplysningerService = OpplysningerService(opplysningerRepository)
    val profileringService = ProfileringService(profileringRepository)
    val bekreftelseService = BekreftelseService(bekreftelseRepository)

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
            val periodeRepositoryMock = mockk<PeriodeRepository>()
            val opplysningerRepositoryMock = mockk<OpplysningerRepository>()
            val profileringRepositoryMock = mockk<ProfileringRepository>()
            val bekreftelseRepositoryMock = mockk<BekreftelseRepository>()
            return ApplicationTestContext(
                periodeRepositoryMock,
                opplysningerRepositoryMock,
                profileringRepositoryMock,
                bekreftelseRepositoryMock
            )
        }

        fun withRealDataAccess(): ApplicationTestContext {
            val dataSource = initTestDatabase()
            Database.connect(dataSource)
            val periodeRepository = PeriodeRepository()
            val opplysningerRepository = OpplysningerRepository()
            val profileringRepository = ProfileringRepository()
            val bekreftelseRepository = BekreftelseRepository()
            return ApplicationTestContext(
                periodeRepository,
                opplysningerRepository,
                profileringRepository,
                bekreftelseRepository
            )
        }
    }
}