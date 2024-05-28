package no.nav.paw.arbeidssoekerregisteret.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

class HealthRoutesTest : FunSpec({
    context("health routes") {
        test("should respond with 200 OK") {
            testApplication {
                routing {
                    healthRoutes(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
                }

                val isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK
                isAliveResponse.body<String>() shouldBe "ALIVE"


                val isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.OK
                isReadyResponse.body<String>() shouldBe "READY"

                val metricsResponse = client.get("/internal/metrics")
                metricsResponse.status shouldBe HttpStatusCode.OK
            }
        }
    }
})