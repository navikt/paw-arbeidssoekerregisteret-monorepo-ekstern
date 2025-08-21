package no.nav.paw.oppslagapi.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.data.query.gjeldeneEllerSisteTidslinje
import no.nav.paw.security.authentication.model.SecurityContext

const val V1_API_BASE_PATH = "/api/v1"

fun Route.v1Routes(
    appQueryLogic: ApplicationQueryLogic
) {
    route(V1_API_BASE_PATH) {
        v1Perioder(appQueryLogic)
        v1VeilederPerioder(appQueryLogic)
    }
}
