package no.nav.paw.client.factory

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule

fun createObjectMapper(): ObjectMapper = jacksonMapperBuilder().configureJackson().build()

fun JsonMapper.Builder.configureJackson(): JsonMapper.Builder = apply {
    addModule(kotlinModule {
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
    })
    accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
    changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    disable(DateTimeFeature.ONE_BASED_MONTHS)
    enable(DateTimeFeature.WRITE_UTC_AS_OFFSET)
    disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
    disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
    disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
}
