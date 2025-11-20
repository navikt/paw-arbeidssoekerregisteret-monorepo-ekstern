package no.nav.paw.oppslagapi.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Route

fun Route.installContentNegotiation(
    configureJackson: ObjectMapper.() -> ObjectMapper
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}