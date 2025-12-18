package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.Metadata
import no.nav.paw.oppslagapi.model.v3.ProfilertTil
import java.util.*

@JsonTypeName("EGENVURDERING_V1")
data class EgenvurderingV4(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("profileringId")
    val profileringId: UUID,

    @field:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @field:JsonProperty("profilertTil")
    val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @field:JsonProperty("egenvurdering")
    val egenvurdering: ProfilertTil = ProfilertTil.UKJENT_VERDI,
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.EGENVURDERING_V1
}