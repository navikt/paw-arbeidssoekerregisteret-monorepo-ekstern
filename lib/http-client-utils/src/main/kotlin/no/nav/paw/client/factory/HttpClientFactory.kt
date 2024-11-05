package no.nav.paw.client.factory

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson

fun createHttpClient(
    engineFactory: HttpClientEngineFactory<HttpClientEngineConfig> = CIO
): HttpClient {
    return HttpClient(engineFactory) {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
}

fun createHttpClient(
    engine: HttpClientEngine
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
}
