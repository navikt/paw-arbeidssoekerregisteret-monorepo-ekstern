package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class TidspunktFraKilde(

    @get:JsonProperty("tidspunkt")
    val tidspunkt: Instant,

    @get:JsonProperty("avviksType")
    val avviksType: AvviksType = AvviksType.UKJENT_VERDI
)