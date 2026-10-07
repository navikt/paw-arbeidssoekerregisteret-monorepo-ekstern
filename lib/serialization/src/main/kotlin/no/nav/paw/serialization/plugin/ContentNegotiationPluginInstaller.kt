package no.nav.paw.serialization.plugin

import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Route
import no.nav.paw.serialization.jackson.configureJackson as configureDefaultJackson
import tools.jackson.databind.json.JsonMapper

fun Application.installContentNegotiationPlugin(
    configureJackson: JsonMapper.Builder.() -> Unit = { configureDefaultJackson() }
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}

fun Route.installContentNegotiationPlugin(
    configureJackson: JsonMapper.Builder.() -> JsonMapper.Builder
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
