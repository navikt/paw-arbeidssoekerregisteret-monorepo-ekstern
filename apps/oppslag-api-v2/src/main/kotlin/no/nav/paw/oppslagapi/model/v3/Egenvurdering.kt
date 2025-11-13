package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Egenvurdering(

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.EGENVURDERING_V1,

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("profileringId")
    val profileringId: UUID,

    @get:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @get:JsonProperty("profilertTil")
    val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @get:JsonProperty("egenvurdering")
    val egenvurdering: ProfilertTil = ProfilertTil.UKJENT_VERDI,
) : Hendelse