package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import java.util.*

@JsonTypeName("PROFILERING_V1")
data class Profilering(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("opplysningerOmArbeidssokerId")
    val opplysningerOmArbeidssokerId: UUID,

    @field:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @field:JsonProperty("profilertTil")
    val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @field:JsonProperty("jobbetSammenhengendeSeksAvTolvSisteMnd")
    val jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean,

    @field:JsonProperty("alder")
    val alder: Int? = null,

    @get:JsonProperty("tidspunkt")
    override val tidspunkt: Instant
) : Hendelse {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.PROFILERING_V1
}