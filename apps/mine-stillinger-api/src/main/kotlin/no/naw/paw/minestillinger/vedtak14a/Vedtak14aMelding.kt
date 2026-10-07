package no.naw.paw.minestillinger.vedtak14a

import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import org.apache.kafka.common.serialization.Deserializer
import java.time.ZonedDateTime

class Siste14aVedtakMelding (
    var aktorId: String? = null,
    var innsatsgruppe: Innsatsgruppe? = null,
    var hovedmal: Hovedmål? = null,
    var fattetDato: ZonedDateTime? = null,
    var fraArena: Boolean = false,
)

data class AktørId(val id: String)

enum class Hovedmål {
    SKAFFE_ARBEID,
    BEHOLDE_ARBEID,
    OKE_DELTAKELSE
}

enum class Innsatsgruppe {
    STANDARD_INNSATS,
    SITUASJONSBESTEMT_INNSATS,
    SPESIELT_TILPASSET_INNSATS,
    GRADERT_VARIG_TILPASSET_INNSATS,
    VARIG_TILPASSET_INNSATS
}

object Siste14aDeserializer: Deserializer<Siste14aVedtakMelding> {
    val objectMapper = jacksonMapperBuilder {
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
    }
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .build()

    override fun deserialize(topic: String?, data: ByteArray?): Siste14aVedtakMelding? {
        if (data == null) return null
        return try {
            objectMapper.readValue(data, Siste14aVedtakMelding::class.java)
        } catch (e: Exception) {
            throw Exception("Feil ved deserialisering av Siste14aVedtakMelding: ${e::class.qualifiedName}")
        }
    }
}
