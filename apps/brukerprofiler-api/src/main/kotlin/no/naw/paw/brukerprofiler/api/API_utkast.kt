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
    JsonSubTypes.Type(StedSoek::class, name = "STED_SOEK_V1"),
    JsonSubTypes.Type(AvansertSoek::class, name = "AVANSERT_SOEK_V1")
)
sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

@JsonTypeName("STED_SOEK_V1")
data class StedSoek(
    override val soekType: StillingssoekType,
    val fylker: List<Fylke>,
    val soekeord: List<String>,
): Stillingssoek

data class Fylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)

@JsonTypeName("AVANSERT_SOEK_V1")
data class AvansertSoek(
    override val soekType: StillingssoekType,
    val hugga: Int,
    val styrk08: String
): Stillingssoek


enum class StillingssoekType {
    STED_SOEK_V1,
    AVANSERT_SOEK_V1
}