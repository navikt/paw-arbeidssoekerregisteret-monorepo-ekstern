package no.naw.paw.brukerprofiler.domain

import no.naw.paw.brukerprofiler.api.vo.ApiKommune

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
