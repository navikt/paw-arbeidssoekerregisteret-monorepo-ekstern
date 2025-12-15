package no.naw.paw.minestillinger.vedtak14a

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())!!

    override fun deserialize(topic: String?, data: ByteArray?): Siste14aVedtakMelding? {
        if (data == null) return null
        return try {
            objectMapper.readValue(data, Siste14aVedtakMelding::class.java)
        } catch (e: Exception) {
            throw Exception("Feil ved deserialisering av Siste14aVedtakMelding: ${e::class.qualifiedName}")
        }
    }
}