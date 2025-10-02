package no.nav.paw.health.startup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.paw.health.StartupCheck
import no.nav.paw.health.hasStarted
import no.nav.paw.health.startupPath
import no.nav.paw.health.startupRoute

class StartupProbeTest : FreeSpec({
    "Alle startup checks er ok" {
        testApplication {
            testApplication(
                hasStarted { true },
                hasStarted { true }
            )
            val client = createClient { }
            val response = client.get(startupPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "Startup check er OK ved ingen checks" {
        testApplication {
            testApplication()
            val client = createClient { }
            val response = client.get(startupPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "Startup check feiler" {
        testApplication {
            testApplication(
                hasStarted { false },
            )
            val client = createClient { }
            val response = client.get(startupPath)
            response.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
    "Startup check feiler så lenge en av sjekkene er false" {
        testApplication {
            testApplication(
                hasStarted { true },
                hasStarted { false }
            )
            val client = createClient { }
            val response = client.get(startupPath)
            response.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
})

fun ApplicationTestBuilder.testApplication(vararg startupChecks: StartupCheck) = application {
    routing {
        startupRoute(*startupChecks)
    }
}


