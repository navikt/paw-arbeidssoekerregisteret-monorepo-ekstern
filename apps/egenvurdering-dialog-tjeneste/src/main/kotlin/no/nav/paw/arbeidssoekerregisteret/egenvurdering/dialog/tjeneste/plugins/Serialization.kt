package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.configureJackson

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
