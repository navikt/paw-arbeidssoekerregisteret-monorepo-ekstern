package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication

class SwaggerRoutesTest : FreeSpec({
    "Skal svare med 200 OK" {
        testApplication {
            environment { config = MapApplicationConfig() }
            routing {
                swaggerRoutes()
            }

            val response = client.get("/docs")
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
