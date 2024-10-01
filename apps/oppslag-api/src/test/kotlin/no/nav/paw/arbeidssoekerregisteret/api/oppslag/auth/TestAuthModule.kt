package no.nav.paw.arbeidssoekerregisteret.api.oppslag.auth

import io.ktor.server.application.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.configureAuthentication
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.configureAuthentication(oAuth2Server: MockOAuth2Server) {
    val config = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE)
    val authProviders =
        config.authProviders.map {
            it.copy(
                discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
                clientId = "default"
            )
        }
    configureAuthentication(authProviders)
}
