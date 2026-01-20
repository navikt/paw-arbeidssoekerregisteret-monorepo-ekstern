package no.nav.paw.oppslagapi.test

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.toURI
import java.nio.file.Path
import java.nio.file.Paths

val v1ApiSpecPath: Path = Paths.get("src/main/resources/openapi/v1-spec.yaml")
val v2ApiSpecPath: Path = Paths.get("src/main/resources/openapi/v2-spec.yaml")
val v3ApiSpecPath: Path = Paths.get("src/main/resources/openapi/v3-spec.yaml")

val v1ApiValidator: OpenApiInteractionValidator = OpenApiInteractionValidator
    .createFor(v1ApiSpecPath.toString())
    .build()
val v2ApiValidator: OpenApiInteractionValidator = OpenApiInteractionValidator
    .createFor(v2ApiSpecPath.toString())
    .build()
val v3ApiValidator: OpenApiInteractionValidator = OpenApiInteractionValidator
    .createFor(v3ApiSpecPath.toString())
    .build()

suspend fun HttpResponse.validateOpenApiSpec(
    validator: OpenApiInteractionValidator
): HttpResponse {
    val simpleResponse = SimpleResponse.Builder(
        status.value
    ).withContentType(contentType()?.let { "$it" })
        .withBody(bodyAsBytes().inputStream())
        .build()
    val resultat = validator.validateResponse(
        this.request.url.toURI().path,
        when (request.method) {
            HttpMethod.Get -> Request.Method.GET
            HttpMethod.Post -> Request.Method.POST
            HttpMethod.Put -> Request.Method.PUT
            HttpMethod.Delete -> Request.Method.DELETE
            HttpMethod.Patch -> Request.Method.PATCH
            else -> throw kotlin.IllegalArgumentException("Ugyldig HTTP-metode for OpenAPI-validering")
        },
        simpleResponse
    )
    withClue(resultat) { resultat.hasErrors() shouldBe false }
    return this
}
