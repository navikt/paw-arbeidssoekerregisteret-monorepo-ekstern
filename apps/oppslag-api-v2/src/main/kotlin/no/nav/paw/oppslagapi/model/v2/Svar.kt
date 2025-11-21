package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Svar(

    @get:JsonProperty("sendtInnAv")
    val sendtInnAv: Metadata,

    @get:JsonProperty("gjelderFra")
    val gjelderFra: Instant,

    @get:JsonProperty("gjelderTil")
    val gjelderTil: Instant,

    @get:JsonProperty("harJobbetIDennePerioden")
    val harJobbetIDennePerioden: Boolean,

    @get:JsonProperty("vilFortsetteSomArbeidssoeker")
    val vilFortsetteSomArbeidssoeker: Boolean
)