package no.nav.paw.oppslagapi.model.v2

import com.fasterxml.jackson.annotation.JsonProperty

enum class HendelseType(val value: String) {

    @JsonProperty(value = "periode_startet_v1")
    periode_startet_v1("periode_startet_v1"),

    @JsonProperty(value = "periode_avsluttet_v1")
    periode_avsluttet_v1("periode_avsluttet_v1"),

    @JsonProperty(value = "opplysninger_v4")
    opplysninger_v4("opplysninger_v4"),

    @JsonProperty(value = "profilering_v1")
    profilering_v1("profilering_v1"),

    @JsonProperty(value = "bekreftelse_v1")
    bekreftelse_v1("bekreftelse_v1"),

    @JsonProperty(value = "pa_vegne_av_start_v1")
    pa_vegne_av_start_v1("pa_vegne_av_start_v1"),

    @JsonProperty(value = "pa_vegne_av_stopp_v1")
    pa_vegne_av_stopp_v1("pa_vegne_av_stopp_v1"),

    @JsonProperty(value = "egenvurdering_v1")
    egenvurdering_v1("egenvurdering_v1");

    override fun toString(): String = value
}

