package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

private val serdeObjectMapper: ObjectMapper
    get() = jacksonObjectMapper {
        withReflectionCacheSize(512)
        disable(KotlinFeature.NullIsSameAsDefault)
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
        enable(KotlinFeature.NullToEmptyCollection)
        enable(KotlinFeature.NullToEmptyMap)
    }.apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        registerModule(JavaTimeModule())
    }

class JsonSerializer<T>(private val objectMapper: ObjectMapper) : Serializer<T> {
    override fun serialize(topic: String?, data: T?): ByteArray {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        } ?: ByteArray(0)
    }
}

class RapporteringsHendelseDeserializer(protected val objectMapper: ObjectMapper) :
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
    constructor() : this(serdeObjectMapper)

    override fun serializer() = JsonSerializer<RapporteringsHendelse>(objectMapper)
    override fun deserializer() = RapporteringsHendelseDeserializer(objectMapper)
}
