package no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

class HealthRoutesTest : FunSpec({
    test("should respond with 200 OK") {
        testApplication {
            routing {
                healthRoutes(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
            }

            val isAliveResponse = client.get("/internal/isAlive")
            isAliveResponse.status shouldBe HttpStatusCode.OK

            val isReadyResponse = client.get("/internal/isReady")
            isReadyResponse.status shouldBe HttpStatusCode.OK

            val metricsResponse = client.get("/internal/metrics")
            metricsResponse.status shouldBe HttpStatusCode.OK
        }
    }
})
