package no.nav.paw.oppslagapi.routes.v1

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

        v1PerioderAggregert(appQueryLogic)
        v1VeilederPerioderAggregert(appQueryLogic)
        v1SamletInformasjon(appQueryLogic)
        v1VeilederSamletInformasjon(appQueryLogic)

        v1Opplysninger(appQueryLogic)
        v1VeilederOpplysninger(appQueryLogic)
        v1Profilering(appQueryLogic)
        v1VeilederProfilering(appQueryLogic)
        v1Bekrefelser(appQueryLogic)
        v1VeilederBekreftelser(appQueryLogic)
    }
}
