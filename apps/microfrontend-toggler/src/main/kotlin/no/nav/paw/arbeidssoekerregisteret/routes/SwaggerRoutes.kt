package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.swaggerRoutes() {
    swaggerUI(path = "/api/docs", swaggerFile = "openapi/docs.yaml")
}
