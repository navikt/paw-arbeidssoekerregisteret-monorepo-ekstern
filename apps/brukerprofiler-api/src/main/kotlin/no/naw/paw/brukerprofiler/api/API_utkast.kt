package no.naw.paw.brukerprofiler.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class Brukerprofil(
    val identitetsnummer: String,
    val kanTilbysTjenestenLedigeStillinger: Boolean,
    val erTjenestenLedigeStillingerAktiv: Boolean,
    val stillingssoek: List<Stillingssoek>,
    val erIkkeInteressert: Boolean,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "soekType",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(StedSoek::class, name = "STED_SOEK_V1"),
    JsonSubTypes.Type(ReiseveiSoek::class, name = "REISEVEI_SOEK_V1")
)
sealed interface Stillingssoek {
    val soekType: StillingssoekType
}

interface HarSoekeord {
    val soekeord: List<String>
}

@JsonTypeName("STED_SOEK_V1")
data class StedSoek(
    override val soekType: StillingssoekType,
    val fylker: List<Fylke>,
    override val soekeord: List<String>,
): Stillingssoek, HarSoekeord

data class Fylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)

@JsonTypeName("REISEVEI_SOEK_V1")
data class ReiseveiSoek(
    override val soekType: StillingssoekType,
    val maksAvstandKm: Int,
    val postnummer: String,
    override val soekeord: List<String>
): Stillingssoek, HarSoekeord


enum class StillingssoekType {
    STED_SOEK_V1,
    REISEVEI_SOEK_V1
}