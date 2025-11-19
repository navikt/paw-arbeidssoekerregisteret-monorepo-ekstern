package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

@JsonTypeName("OPPLYSNINGER_V4")
data class OpplysningerOmArbeidssoeker(

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.OPPLYSNINGER_V4,

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @field:JsonProperty("utdanning")
    val utdanning: Utdanning? = null,

    @field:JsonProperty("helse")
    val helse: Helse? = null,

    @field:JsonProperty("jobbsituasjon")
    val jobbsituasjon: Jobbsituasjon? = null,

    @field:JsonProperty("annet")
    val annet: Annet? = null
) : Hendelse