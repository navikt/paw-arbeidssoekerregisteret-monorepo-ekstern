package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class OpplysningerOmArbeidssoeker(

    @get:JsonProperty("id")
    val id: UUID,

    @get:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @get:JsonProperty("utdanning")
    val utdanning: Utdanning? = null,

    @get:JsonProperty("helse")
    val helse: Helse? = null,

    @get:JsonProperty("jobbsituasjon")
    val jobbsituasjon: Jobbsituasjon? = null,

    @get:JsonProperty("annet")
    val annet: Annet? = null
) : Hendelse {

    @get:JsonProperty("type")
    override val type: HendelseType = HendelseType.OPPLYSNINGER_V4
}