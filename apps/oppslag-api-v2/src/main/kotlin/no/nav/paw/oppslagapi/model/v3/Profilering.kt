package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Profilering(

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("opplysningerOmArbeidssokerId")
    val opplysningerOmArbeidssokerId: UUID,

    @get:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @get:JsonProperty("profilertTil")
    val profilertTil: ProfilertTil = ProfilertTil.UKJENT_VERDI,

    @get:JsonProperty("jobbetSammenhengendeSeksAvTolvSisteMnd")
    val jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean,

    @get:JsonProperty("alder")
    val alder: Int? = null
) : Hendelse {

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.PROFILERING_V1
}