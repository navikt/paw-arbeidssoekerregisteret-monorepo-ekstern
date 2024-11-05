package no.nav.paw.pdl.factory

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.config.PDL_CLIENT_CONFIG
import no.nav.paw.pdl.config.PdlClientConfig

fun createPdlClient(
    httpClient: HttpClient = httpClient(),
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): PdlClient {
    val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
    val azureAdM2MTokenClient = createAzureAdM2MTokenClient(
        runtimeEnvironment,
        loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    )
    return PdlClient(
        url = pdlClientConfig.url,
        tema = pdlClientConfig.tema,
        httpClient = httpClient
    ) { azureAdM2MTokenClient.createMachineToMachineToken(pdlClientConfig.scope) }
}

private fun httpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
}

fun createObjectMapper(): ObjectMapper {
    return jacksonObjectMapper().apply {
        configureJackson()
    }
}

private fun ObjectMapper.configureJackson() {
    setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    registerModule(JavaTimeModule())
    kotlinModule {
        withReflectionCacheSize(512)
        disable(KotlinFeature.NullIsSameAsDefault)
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
        enable(KotlinFeature.NullToEmptyCollection)
        enable(KotlinFeature.NullToEmptyMap)
    }
}
