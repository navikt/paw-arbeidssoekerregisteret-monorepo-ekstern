package no.naw.paw.minestillinger.domain

import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiKommune

data class Fylke(
    val navn: String,
    val fylkesnummer: String,
    val kommuner: List<Kommune> = emptyList(),
)

fun Fylke.api(): ApiFylke = ApiFylke(
    navn = navn,
    fylkesnummer = fylkesnummer,
    kommuner = kommuner.map { it.api() }
)

fun ApiFylke.domain(): Fylke = Fylke(
    navn = navn,
    kommuner = kommuner.map(ApiKommune::domain),
    fylkesnummer = fylkesnummer,
)
