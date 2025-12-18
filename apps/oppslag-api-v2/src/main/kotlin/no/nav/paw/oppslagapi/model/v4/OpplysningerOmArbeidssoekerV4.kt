package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.Annet
import no.nav.paw.oppslagapi.model.v3.Helse
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.Jobbsituasjon
import no.nav.paw.oppslagapi.model.v3.Metadata
import no.nav.paw.oppslagapi.model.v3.Utdanning
import java.util.*

@JsonTypeName("OPPLYSNINGER_V4")
data class OpplysningerOmArbeidssoekerV4(

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
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.OPPLYSNINGER_V4
}