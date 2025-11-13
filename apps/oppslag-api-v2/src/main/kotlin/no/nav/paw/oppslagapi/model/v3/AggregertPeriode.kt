package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class AggregertPeriode(

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @get:JsonProperty("startet")
    val startet: Metadata,

    @get:JsonProperty("avsluttet")
    val avsluttet: Metadata? = null,

    @get:JsonProperty("opplysning")
    val opplysning: OpplysningerOmArbeidssoeker? = null,

    @get:JsonProperty("profilering")
    val profilering: Profilering? = null,

    @get:JsonProperty("egenvurdering")
    val egenvurdering: Egenvurdering? = null,

    @get:JsonProperty("bekreftelse")
    val bekreftelse: Bekreftelse? = null
)
