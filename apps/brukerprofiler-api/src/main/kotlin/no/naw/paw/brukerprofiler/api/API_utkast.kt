package no.naw.paw.brukerprofiler.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class Brukerprofil(
    val identitetsnummer: String,
    val kanTilbysTjenestenLedigeStillinger: Boolean,
    val erTjenestenLedigeStillingerAktiv: Boolean,
    val stillingssoek: List<Stillingssoek>
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "soekType",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(SimpeltSoek::class, name = "SIMPELT_SOEK_V1")
)
sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

@JsonTypeName("SIMPELT_SOEK_V1")
data class SimpeltSoek(
    override val soekType: StillingssoekType,
    val kommune: String,
    val styrk08: String
): Stillingssoek


enum class StillingssoekType {
    SIMPELT_SOEK_V1
}