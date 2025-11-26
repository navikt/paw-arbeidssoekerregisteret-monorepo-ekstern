package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.route

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.openApiRoutes() {
    swaggerUI("/api/docs/v1", "openapi/v1.yaml")
}