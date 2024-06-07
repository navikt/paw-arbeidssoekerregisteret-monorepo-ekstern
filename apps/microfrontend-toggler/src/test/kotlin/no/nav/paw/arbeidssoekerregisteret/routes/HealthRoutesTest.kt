package no.nav.paw.arbeidssoekerregisteret.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.StandardHealthIndicator
import no.nav.paw.arbeidssoekerregisteret.model.HealthStatus

class HealthRoutesTest : FreeSpec({
    with(HealthRoutesTestContext()) {
        "Test av health routes" {
            testApplication {
                routing {
                    healthRoutes(livenessHealthIndicator, readinessHealthIndicator, meterRegistry)
                }

                // Health indicators er default UNKNOWN

                val metricsResponse = client.get("/internal/metrics")
                metricsResponse.status shouldBe HttpStatusCode.OK

                var isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK
                isAliveResponse.body<String>() shouldBe HealthStatus.HEALTHY.value

                var isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                isReadyResponse.body<String>() shouldBe HealthStatus.UNKNOWN.value

                // Setter readiness health indicator til HEALTHY
                readinessHealthIndicator.setHealthy()

                isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK
                isAliveResponse.body<String>() shouldBe HealthStatus.HEALTHY.value

                isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.OK
                isReadyResponse.body<String>() shouldBe HealthStatus.HEALTHY.value

                // Setter readiness health indicator til UNHEALTHY
                readinessHealthIndicator.setUnhealthy()

                isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK
                isAliveResponse.body<String>() shouldBe HealthStatus.HEALTHY.value

                isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                isReadyResponse.body<String>() shouldBe HealthStatus.UNHEALTHY.value

                // Setter readiness health indicator tilbake til UNKNOWN
                readinessHealthIndicator.setUnknown()

                isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK
                isAliveResponse.body<String>() shouldBe HealthStatus.HEALTHY.value

                isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                isReadyResponse.body<String>() shouldBe HealthStatus.UNKNOWN.value


                // Setter liveness health indicator til UNHEALTHY
                livenessHealthIndicator.setUnhealthy()

                isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                isAliveResponse.body<String>() shouldBe HealthStatus.UNHEALTHY.value

                isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                isReadyResponse.body<String>() shouldBe HealthStatus.UNKNOWN.value
            }
        }
    }
})

class HealthRoutesTestContext {
    val livenessHealthIndicator = StandardHealthIndicator(HealthStatus.HEALTHY)
    val readinessHealthIndicator = StandardHealthIndicator(HealthStatus.UNKNOWN)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}