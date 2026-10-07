package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy

val buildObjectMapper: ObjectMapper
    get() = jacksonMapperBuilder {
        withReflectionCacheSize(512)
        disable(KotlinFeature.NullIsSameAsDefault)
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
    }.configureJackson().build()

fun JsonMapper.Builder.configureJackson(): JsonMapper.Builder = apply {
    accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
    disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
    changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
}
