package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes

class MetricsRouteTest : FreeSpec({
    "Skal svare med 200 OK" {
        testApplication {
            routing {
                metricsRoutes(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
            }

            val metricsResponse = client.get("/internal/metrics")
            metricsResponse.status shouldBe HttpStatusCode.OK
        }
    }
})
