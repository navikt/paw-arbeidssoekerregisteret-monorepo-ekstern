package no.naw.paw.ledigestillinger

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val ledigeStillingerApiObjectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, false)
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)