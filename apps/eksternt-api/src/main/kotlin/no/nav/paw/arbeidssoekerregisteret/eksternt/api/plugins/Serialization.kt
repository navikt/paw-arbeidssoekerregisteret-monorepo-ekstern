package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            addModule(kotlinModule {
                disable(KotlinFeature.SingletonSupport)
                disable(KotlinFeature.StrictNullChecks)
            })
            disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
            disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        }
    }
}
