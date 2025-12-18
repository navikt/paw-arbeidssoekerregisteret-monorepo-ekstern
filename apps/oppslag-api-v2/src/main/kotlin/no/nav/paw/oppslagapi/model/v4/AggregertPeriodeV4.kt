package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class AggregertPeriodeV4(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("identitetsnummer")
    val identitetsnummer: String,

    @field:JsonProperty("startet")
    val startet: PeriodeStartetV4,

    @field:JsonProperty("avsluttet")
    val avsluttet: PeriodeAvluttetV4? = null,

    @field:JsonProperty("opplysning")
    val opplysning: OpplysningerOmArbeidssoekerV4? = null,

    @field:JsonProperty("profilering")
    val profilering: ProfileringV4? = null,

    @field:JsonProperty("egenvurdering")
    val egenvurdering: EgenvurderingV4? = null,

    @field:JsonProperty("bekreftelse")
    val bekreftelse: BekreftelseV4? = null
)