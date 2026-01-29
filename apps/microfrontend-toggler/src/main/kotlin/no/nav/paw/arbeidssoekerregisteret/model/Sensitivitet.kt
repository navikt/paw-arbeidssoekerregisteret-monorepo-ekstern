package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonValue

enum class Sensitivitet(@get:JsonValue val value: String) {
    HIGH("high"),
    SUBSTANTIAL("substantial")
}