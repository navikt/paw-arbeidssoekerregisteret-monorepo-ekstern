package no.nav.paw.arbeidssoekerregisteret.model

import io.ktor.http.HttpStatusCode

/**
 * Object som inneholder detaljer om en oppstått feilsituasjon, basert på RFC 7807.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">IETF RFC 7807</a>
 */
data class ProblemDetails(
    val type: String,
    val title: String,
    val status: HttpStatusCode,
    val detail: String,
    val instance: String
) {
    constructor(
        title: String,
        status: HttpStatusCode,
        detail: String,
        instance: String
    ) : this("about:blank", title, status, detail, instance)
}