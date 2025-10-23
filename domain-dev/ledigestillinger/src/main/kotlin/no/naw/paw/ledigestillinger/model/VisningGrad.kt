package no.naw.paw.ledigestillinger.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class VisningGrad(val value: String) {

    @JsonProperty(value = "UBEGRENSET")
    UBEGRENSET("UBEGRENSET"),

    @JsonProperty(value = "BEGRENSET_INTERNT")
    BEGRENSET_INTERNT("BEGRENSET_INTERNT"),

    @JsonProperty(value = "BEGRENSET_ARBEIDSGIVER")
    BEGRENSET_ARBEIDSGIVER("BEGRENSET_ARBEIDSGIVER"),

    @JsonProperty(value = "BEGRENSET_KILDE")
    BEGRENSET_KILDE("BEGRENSET_KILDE");
}

