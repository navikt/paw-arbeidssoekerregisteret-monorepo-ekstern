package no.naw.paw.ledigestillinger

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy

val ledigeStillingerApiObjectMapper: ObjectMapper = jacksonMapperBuilder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    .configure(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, false)
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false)
    .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    .disable(DateTimeFeature.ONE_BASED_MONTHS)
    .enable(DateTimeFeature.WRITE_UTC_AS_OFFSET)
    .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
    .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
    .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
    .build()
