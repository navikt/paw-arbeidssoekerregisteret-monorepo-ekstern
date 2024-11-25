package no.nav.paw.error.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.serialize.HttpStatusCodeDeserializer
import no.nav.paw.error.serialize.HttpStatusCodeSerializer
import java.net.URI
import java.time.Instant
import java.util.*

/**
 * Object som inneholder detaljer om en oppstått feilsituasjon, basert på RFC 9457.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">IETF RFC 9457</a>
 */
data class ProblemDetails(
    val id: UUID = UUID.randomUUID(),
    val type: URI = ErrorTypeBuilder.default(),
    @JsonSerialize(using = HttpStatusCodeSerializer::class) @JsonDeserialize(using = HttpStatusCodeDeserializer::class) val status: HttpStatusCode,
    val title: String,
    val detail: String? = null,
    val instance: String,
    val timestamp: Instant = Instant.now()
)

class ErrorTypeBuilder private constructor(
    var team: String = "paw",
    var domain: String = "default",
    var error: String = "ukjent-feil",
) {
    fun team(team: String) = apply { this.team = team }
    fun domain(domain: String) = apply { this.domain = domain }
    fun error(error: String) = apply { this.error = error }
    fun build(): URI = URI.create("urn:${team.lowercase()}:${domain.lowercase()}:${error.lowercase()}")

    companion object {
        fun builder(): ErrorTypeBuilder = ErrorTypeBuilder()
        fun default(): URI = ErrorTypeBuilder().build()
    }
}

class ProblemDetailsBuilder private constructor(
    var type: URI = ErrorTypeBuilder.default(),
    var status: HttpStatusCode = HttpStatusCode.InternalServerError,
    var title: String? = null,
    var detail: String? = null,
    var instance: String = "/"
) {
    fun type(type: URI) = apply { this.type = type }
    fun status(status: HttpStatusCode) = apply { this.status = status }
    fun title(title: String) = apply { this.title = title }
    fun detail(detail: String) = apply { this.detail = detail }
    fun instance(instance: String) = apply { this.instance = instance }
    fun build(): ProblemDetails = ProblemDetails(
        type = type,
        status = status,
        title = title ?: status.description,
        detail = detail,
        instance = instance
    )

    companion object {
        fun builder(): ProblemDetailsBuilder = ProblemDetailsBuilder()
    }
}