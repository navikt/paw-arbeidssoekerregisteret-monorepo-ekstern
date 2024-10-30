package no.nav.paw.arbeidssokerregisteret.arena.adapter.health

import io.ktor.http.*

data class Status(
    val code: HttpStatusCode,
    val message: String
)