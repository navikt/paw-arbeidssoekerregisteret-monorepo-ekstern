package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*

fun createHttpClient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        jackson()
    }
}
