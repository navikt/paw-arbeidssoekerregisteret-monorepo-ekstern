package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Hendelse(

    @get:JsonProperty("hendelseType")
    val hendelseType: HendelseType,

    @get:JsonProperty("tidspunkt")
    val tidspunkt: Instant,

    @get:JsonProperty("periodeStartetV1")
    val periodeStartetV1: Metadata? = null,

    @get:JsonProperty("periodeAvsluttetV1")
    val periodeAvsluttetV1: Metadata? = null,

    @get:JsonProperty("opplysningerV4")
    val opplysningerV4: OpplysningerOmArbeidssoeker? = null,

    @get:JsonProperty("profileringV1")
    val profileringV1: Profilering? = null,

    @get:JsonProperty("bekreftelseV1")
    val bekreftelseV1: BekreftelseMedMetadata? = null,

    @get:JsonProperty("paVegneAvStartV1")
    val paVegneAvStartV1: PaaVegneAvStart? = null,

    @get:JsonProperty("paVegneAvStoppV1")
    val paVegneAvStoppV1: PaaVegneAvStopp? = null,

    @get:JsonProperty("egenvurderingV1")
    val egenvurderingV1: Egenvurdering? = null
)