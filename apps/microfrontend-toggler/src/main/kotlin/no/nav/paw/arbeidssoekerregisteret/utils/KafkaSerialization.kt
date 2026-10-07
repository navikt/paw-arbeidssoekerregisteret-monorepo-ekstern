package no.nav.paw.arbeidssoekerregisteret.utils

import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

inline fun <reified T> buildJsonSerializer(
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    objectMapper: ObjectMapper = buildObjectMapper
) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JacksonException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonDeserializer(
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    objectMapper: ObjectMapper = buildObjectMapper
) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue<T>(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JacksonException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonSerde(
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    objectMapper: ObjectMapper = buildObjectMapper
) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(runtimeEnvironment, objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(runtimeEnvironment, objectMapper)
    }
}

fun buildPeriodeInfoSerde() = buildJsonSerde<PeriodeInfo>()
fun buildToggleSerde() = buildJsonSerde<Toggle>()
