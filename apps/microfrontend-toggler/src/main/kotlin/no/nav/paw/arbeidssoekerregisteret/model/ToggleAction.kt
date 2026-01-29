package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ToggleAction(@get:JsonValue val value: String) {
    ENABLE("enable"),
    DISABLE("disable")
}