package no.nav.paw.oppslagapi.model.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Svar(

    @field:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @field:JsonProperty("gjelderFra")
    val gjelderFra: Instant,

    @field:JsonProperty("gjelderTil")
    val gjelderTil: Instant,

    @field:JsonProperty("harJobbetIDennePerioden")
    val harJobbetIDennePerioden: Boolean,

    @field:JsonProperty("vilFortsetteSomArbeidssoeker")
    val vilFortsetteSomArbeidssoeker: Boolean
)
