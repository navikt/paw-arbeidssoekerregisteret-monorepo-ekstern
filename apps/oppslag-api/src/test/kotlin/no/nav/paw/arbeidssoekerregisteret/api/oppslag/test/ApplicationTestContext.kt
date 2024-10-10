package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer.PdlHttpConsumer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.AuthorizationService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.OpplysningerService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.ProfileringService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.security.mock.oauth2.MockOAuth2Server

open class ApplicationTestContext {

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val mockOAuth2Server = MockOAuth2Server()
    val pdlHttpConsumerMock: PdlHttpConsumer = mockk<PdlHttpConsumer>()
    val poaoTilgangHttpClientMock: PoaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()
    val authorizationService: AuthorizationService = AuthorizationService(
        pdlHttpConsumerMock,
        poaoTilgangHttpClientMock
    )
    val periodeRepositoryMock = mockk<PeriodeRepository>()
    val opplysningerRepositoryMock = mockk<OpplysningerRepository>()
    val profileringRepositoryMock = mockk<ProfileringRepository>()
    val periodeService = PeriodeService(periodeRepositoryMock)
    val opplysningerService = OpplysningerService(opplysningerRepositoryMock)
    val profileringService = ProfileringService(profileringRepositoryMock)

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }
}