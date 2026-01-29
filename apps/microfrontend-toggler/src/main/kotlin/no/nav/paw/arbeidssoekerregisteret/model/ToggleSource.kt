package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ToggleSource(@get:JsonValue val value: String) {
    ARBEIDSSOEKERPERIODE("arbeidsøkerperiode"),
    SISTE_14A_VEDTAK("siste-14a-vedtak")
}