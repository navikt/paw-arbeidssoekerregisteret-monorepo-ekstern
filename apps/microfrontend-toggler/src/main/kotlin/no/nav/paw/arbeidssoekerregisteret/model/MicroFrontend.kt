package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonValue

enum class MicroFrontend(@get:JsonValue val value: String) {
    AIA_MIN_SIDE("aia-min-side")
}