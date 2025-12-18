package no.nav.paw.oppslagapi.model.v4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.paw.oppslagapi.model.v3.BekreftelsStatus
import no.nav.paw.oppslagapi.model.v3.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.Svar
import java.util.*

@JsonTypeName("BEKREFTELSE_V1")
data class BekreftelseV4(

    @field:JsonProperty("id")
    val id: UUID,

    @field:JsonProperty("bekreftelsesloesning")
    val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @field:JsonProperty("status")
    val status: BekreftelsStatus,

    @field:JsonProperty("svar")
    val svar: Svar
) : HendelseV4 {

    @field:JsonProperty("type")
    override val type: HendelseType = HendelseType.BEKREFTELSE_V1
}