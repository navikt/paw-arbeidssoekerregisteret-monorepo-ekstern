package no.nav.paw.oppslagapi.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic

const val V1_API_BASE_PATH = "/api/v1"

fun Route.v1Routes(
    appQueryLogic: ApplicationQueryLogic
) {
    route(V1_API_BASE_PATH) {
        v1Perioder(appQueryLogic)
        v1VeilederPerioder(appQueryLogic)
    }
}