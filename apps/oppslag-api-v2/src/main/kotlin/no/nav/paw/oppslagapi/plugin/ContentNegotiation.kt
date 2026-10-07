package no.nav.paw.oppslagapi.plugin

import tools.jackson.databind.json.JsonMapper
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Route

fun Route.installContentNegotiation(
    configureJackson: JsonMapper.Builder.() -> JsonMapper.Builder
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
