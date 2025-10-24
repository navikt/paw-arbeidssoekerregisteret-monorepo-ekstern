package no.naw.paw.minestillinger.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate

data class ApiLedigeStillinger(
    val soek: ApiStillingssoek,
    val frorrigeSoek: Instant?,
    val resultat: List<ApiJobbAnnonse>
)

data class ApiJobbAnnonse(
    val tittel: String,
    val stillingbeskrivelse: String?,
    val publisert: Instant,
    val soeknadsfrist: Soeknadsfrist,
    val land: String,
    val kommune: String?,
    val sektor: Sektor,
    val selskap: String
)

data class Soeknadsfrist(
    val raw: String,
    @JsonProperty("fristType")
    val type: SoeknadsfristType,
    val dato: LocalDate?
)

enum class SoeknadsfristType {
    Ukjent, Snarest, Dato, Fortloepende;
}

enum class Sektor {
    Offentlig, Privat, Ukjent
}