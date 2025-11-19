package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class AggregertPeriode(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @field:JsonProperty("startet")
    val startet: PeriodeStartet,

    @field:JsonProperty("avsluttet")
    val avsluttet: PeriodeAvluttet? = null,

    @field:JsonProperty("opplysning")
    val opplysning: OpplysningerOmArbeidssoeker? = null,

    @field:JsonProperty("profilering")
    val profilering: Profilering? = null,

    @field:JsonProperty("egenvurdering")
    val egenvurdering: Egenvurdering? = null,

    @field:JsonProperty("bekreftelse")
    val bekreftelse: Bekreftelse? = null
)
