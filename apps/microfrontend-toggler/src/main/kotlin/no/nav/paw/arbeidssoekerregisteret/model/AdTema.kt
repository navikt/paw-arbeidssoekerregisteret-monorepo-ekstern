package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonValue

enum class AdTema(@get:JsonValue val value: String) {
    OPP("OPP")
}