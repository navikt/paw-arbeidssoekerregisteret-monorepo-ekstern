package no.naw.paw.minestillinger

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
import java.nio.file.Paths

suspend fun HttpResponse.validateAgainstOpenApiSpec(): HttpResponse {
    val simpleResponse = SimpleResponse.Builder(
        status.value
    ).withContentType(contentType()?.let { "$it" })
        .withBody(bodyAsBytes().inputStream())
        .build()
    val resultat = specValidator.validateResponse(
        this.request.url.toURI().path,
        when (request.method) {
            HttpMethod.Get -> Request.Method.GET
            HttpMethod.Post -> Request.Method.POST
            HttpMethod.Put -> Request.Method.PUT
            HttpMethod.Delete -> Request.Method.DELETE
            HttpMethod.Patch -> Request.Method.PATCH
            else -> throw IllegalArgumentException("Ugyldig HTTP-metode for OpenAPI-validering")
        },
        simpleResponse
    )
    withClue(resultat) { resultat.hasErrors() shouldBe false }
    return this
}

private val openApiSpecPath = Paths.get("src/main/resources/openapi/openapi-spec.yaml")
private val specValidator = OpenApiInteractionValidator.createFor(openApiSpecPath.toString()).build()
