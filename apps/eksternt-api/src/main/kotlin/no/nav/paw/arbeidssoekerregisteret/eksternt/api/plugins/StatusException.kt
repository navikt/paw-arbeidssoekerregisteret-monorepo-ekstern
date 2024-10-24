package no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins

import io.ktor.http.HttpStatusCode

open class StatusException(val status: HttpStatusCode, val description: String? = null) :
    Exception("Request failed with status: $status. Description: $description")
