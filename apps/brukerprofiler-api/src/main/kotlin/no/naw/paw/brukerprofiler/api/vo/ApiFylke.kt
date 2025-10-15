package no.naw.paw.brukerprofiler.api.vo

import no.naw.paw.brukerprofiler.domain.Fylke

data class ApiFylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)

fun ApiFylke.domain() = Fylke(
    navn = navn,
    kommuner = kommuner,
)