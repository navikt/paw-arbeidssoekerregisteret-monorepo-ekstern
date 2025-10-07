package no.nav.paw.client.factory

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson

fun createHttpClient(
    logLevel: LogLevel = LogLevel.NONE,
    engineFactory: HttpClientEngineFactory<HttpClientEngineConfig> = CIO
): HttpClient {
    return HttpClient(engineFactory) {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
        install(Logging) {
            level = logLevel
        }
    }
}
