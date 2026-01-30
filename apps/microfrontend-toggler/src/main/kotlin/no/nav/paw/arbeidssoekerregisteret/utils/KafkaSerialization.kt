package no.nav.paw.arbeidssoekerregisteret.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class ToggleJsonSerializer(
    private val delegate: Serializer<Toggle> = buildJsonSerializer()
) : Serializer<Toggle> {
    override fun serialize(topic: String?, data: Toggle?): ByteArray {
        return delegate.serialize(topic, data)
    }
}

inline fun <reified T> buildJsonSerializer(
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    objectMapper: ObjectMapper = buildObjectMapper
) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
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
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
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
fun buildSiste14aVedtakSerde() = buildJsonSerde<Siste14aVedtak>()
fun buildBeriket14aVedtakSerde() = buildJsonSerde<Beriket14aVedtak>()
fun buildToggleSerde() = buildJsonSerde<Toggle>()
fun buildToggleStateSerde() = buildJsonSerde<ToggleState>()