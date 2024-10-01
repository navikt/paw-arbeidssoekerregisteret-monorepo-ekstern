package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*

class SwaggerRoutesTest : FunSpec({
    test("should respond with 200 OK") {
        testApplication {
            routing {
                swaggerRoutes()
            }

            val response = client.get("/docs")
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
