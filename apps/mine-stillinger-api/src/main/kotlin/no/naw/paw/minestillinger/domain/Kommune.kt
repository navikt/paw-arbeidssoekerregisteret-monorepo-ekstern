package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.vo.ApiKommune

data class Kommune(
    val kommunenummer: String,
    val navn: String
)

fun Kommune.api() = ApiKommune(
    kommunenummer = kommunenummer,
    navn = navn
)

fun ApiKommune.domain() = Kommune(
    kommunenummer = kommunenummer,
    navn = navn
)
