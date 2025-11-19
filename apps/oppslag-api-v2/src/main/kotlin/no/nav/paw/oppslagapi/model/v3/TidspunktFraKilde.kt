package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class TidspunktFraKilde(

    @field:JsonProperty("tidspunkt")
    val tidspunkt: Instant,

    @field:JsonProperty("avviksType")
    val avviksType: AvviksType = AvviksType.UKJENT_VERDI
)