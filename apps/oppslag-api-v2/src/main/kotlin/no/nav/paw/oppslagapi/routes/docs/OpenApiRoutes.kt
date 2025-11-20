package no.nav.paw.oppslagapi.routes.docs

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.utils.configureJacksonForV3

fun Route.openApiRoutes() {
    route("/api/docs") {
        installContentNegotiation {
            configureJacksonForV3()
        }
        installErrorHandler()

        swaggerUI(path = "/v1", swaggerFile = "openapi/v1-spec.yaml")
        swaggerUI(path = "/v2", swaggerFile = "openapi/v2-spec.yaml")
        swaggerUI(path = "/v3", swaggerFile = "openapi/v3-spec.yaml")
    }
}