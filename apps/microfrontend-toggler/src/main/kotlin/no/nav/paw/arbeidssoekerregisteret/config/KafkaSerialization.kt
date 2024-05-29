package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.rapportering.internehendelser.EksternGracePeriodeUtloept
import no.nav.paw.rapportering.internehendelser.LeveringsfristUtloept
import no.nav.paw.rapportering.internehendelser.PeriodeAvsluttet
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsMeldingMottatt
import no.nav.paw.rapportering.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.rapportering.internehendelser.eksternGracePeriodeUtloeptHendelseType
import no.nav.paw.rapportering.internehendelser.leveringsfristUtloeptHendelseType
import no.nav.paw.rapportering.internehendelser.meldingMottattHendelseType
import no.nav.paw.rapportering.internehendelser.periodeAvsluttetHendelsesType
import no.nav.paw.rapportering.internehendelser.rapporteringTilgjengeligHendelseType
import no.nav.paw.rapportering.internehendelser.registerGracePeriodeUtloeptHendelseType
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class ToggleJsonSerializer(private val delegate: Serializer<Toggle>) : Serializer<Toggle> {
    constructor() : this(buildJsonSerializer())

    override fun serialize(topic: String?, data: Toggle?): ByteArray {
        return delegate.serialize(topic, data)
    }
}

inline fun <reified T> buildJsonSerializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonSerializer(): Serializer<T> = buildJsonSerializer<T>(currentNaisEnv, buildObjectMapper)

inline fun <reified T> buildJsonDeserializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue<T>(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonDeserializer(): Deserializer<T> =
    buildJsonDeserializer<T>(currentNaisEnv, buildObjectMapper)

inline fun <reified T> buildJsonSerde(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(naisEnv, objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(naisEnv, objectMapper)
    }
}

inline fun <reified T> buildJsonSerde(): Serde<T> {
    return buildJsonSerde<T>(currentNaisEnv, buildObjectMapper)
}

fun buildToggleSerde(): Serde<Toggle> = buildJsonSerde<Toggle>()

fun buildToggleStateSerde(): Serde<ToggleState> = buildJsonSerde<ToggleState>()

class RapporteringsHendelseDeserializer(private val naisEnv: NaisEnv, private val objectMapper: ObjectMapper) :
    Deserializer<RapporteringsHendelse> {
    override fun deserialize(topic: String?, data: ByteArray?): RapporteringsHendelse? {
        if (data == null) return null
        val node = objectMapper.readTree(data)
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            rapporteringTilgjengeligHendelseType -> objectMapper.readValue<RapporteringTilgjengelig>(node.traverse())
            meldingMottattHendelseType -> objectMapper.readValue<RapporteringsMeldingMottatt>(node.traverse())
            periodeAvsluttetHendelsesType -> objectMapper.readValue<PeriodeAvsluttet>(node.traverse())
            leveringsfristUtloeptHendelseType -> objectMapper.readValue<LeveringsfristUtloept>(node.traverse())
            registerGracePeriodeUtloeptHendelseType -> objectMapper.readValue<RegisterGracePeriodeUtloept>(node.traverse())
            eksternGracePeriodeUtloeptHendelseType -> objectMapper.readValue<EksternGracePeriodeUtloept>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent hendelse type: '$hendelseType'")
        }
    }
}

class RapporteringsHendelseSerde(naisEnv: NaisEnv, private val objectMapper: ObjectMapper) :
    Serde<RapporteringsHendelse> {
    constructor() : this(currentNaisEnv, buildObjectMapper)

    override fun serializer() = buildJsonSerializer<RapporteringsHendelse>(currentNaisEnv, objectMapper)
    override fun deserializer() = RapporteringsHendelseDeserializer(currentNaisEnv, objectMapper)
}
