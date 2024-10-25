package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.EksternRequest
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.testModule

class ArbeidssoekerRoutesTest : FreeSpec({
    "should respond with 200 OK" {
        testApplication {
            environment {
                config =
                    MapApplicationConfig().apply {
                        put("ktor.application.modules.0", "no.nav.paw.arbeidssoekerregisteret.eksternt.api.TestApplicationKt.testModule")
                    }
            }
            application {
                testModule()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val responseFraStartetDato =
                client.post("/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(EksternRequest("12345678911", "2021-01-01"))
                }

            val response =
                client.post("/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(EksternRequest("12345678911"))
                }

            responseFraStartetDato.status shouldBe HttpStatusCode.OK
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "should respond with 400 BadRequest" {
        testApplication {
            environment {
                config =
                    MapApplicationConfig().apply {
                        put("ktor.application.modules.0", "no.nav.paw.arbeidssoekerregisteret.eksternt.api.TestApplicationKt.testModule")
                    }
            }
            application {
                testModule()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                registerModule(JavaTimeModule())
                            }
                        }
                    }
                }

            val wrongDateFormattingResponse =
                client.post("/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(EksternRequest("12345678911", "01-01-2021"))
                }

            val wrongRequestBodyResponse =
                client.post("/arbeidssoekerperioder") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        "wrongRequestBody"
                    )
                }

            wrongDateFormattingResponse.status shouldBe HttpStatusCode.BadRequest
            wrongDateFormattingResponse.bodyAsText() shouldBe "Ugyldig dato 'fraStartetDato' må være satt med yyyy-mm-dd"

            wrongRequestBodyResponse.status shouldBe HttpStatusCode.BadRequest
            wrongRequestBodyResponse.bodyAsText() shouldBe "Ugyldig request body"
        }
    }
})
