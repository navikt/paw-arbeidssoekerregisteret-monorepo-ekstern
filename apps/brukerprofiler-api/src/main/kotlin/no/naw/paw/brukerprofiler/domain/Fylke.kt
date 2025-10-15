package no.naw.paw.brukerprofiler.domain

import no.naw.paw.brukerprofiler.api.vo.ApiFylke

data class Fylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)

fun Fylke.api(): ApiFylke = ApiFylke(
    navn = navn,
    kommuner = kommuner
)

fun ApiFylke.domain(): Fylke = Fylke(
    navn = navn,
    kommuner = kommuner
)