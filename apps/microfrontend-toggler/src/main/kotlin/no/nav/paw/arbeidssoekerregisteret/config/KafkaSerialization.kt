package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
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

inline fun <reified T> buildJsonSerializer(objectMapper: ObjectMapper) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        return objectMapper.writeValueAsBytes(data)
    }
}

inline fun <reified T> buildJsonSerializer(): Serializer<T> = buildJsonSerializer<T>(buildObjectMapper)

inline fun <reified T> buildJsonDeserializer(objectMapper: ObjectMapper) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        return objectMapper.readValue<T>(data)
    }
}

inline fun <reified T> buildJsonDeserializer(): Deserializer<T> = buildJsonDeserializer<T>(buildObjectMapper)

inline fun <reified T> buildJsonSerde(objectMapper: ObjectMapper) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(objectMapper)
    }
}

inline fun <reified T> buildJsonSerde(): Serde<T> {
    return buildJsonSerde<T>(buildObjectMapper)
}

fun buildToggleSerde(): Serde<Toggle> = buildJsonSerde<Toggle>()

fun buildToggleStateSerde(): Serde<ToggleState> = buildJsonSerde<ToggleState>()

class RapporteringsHendelseDeserializer(private val objectMapper: ObjectMapper) :
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

class RapporteringsHendelseSerde(private val objectMapper: ObjectMapper) : Serde<RapporteringsHendelse> {
    constructor() : this(buildObjectMapper)

    override fun serializer() = buildJsonSerializer<RapporteringsHendelse>(objectMapper)
    override fun deserializer() = RapporteringsHendelseDeserializer(objectMapper)
}
