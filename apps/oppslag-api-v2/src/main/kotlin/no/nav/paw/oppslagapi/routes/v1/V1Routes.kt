package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic

fun Route.v1Routes(
    appQueryLogic: ApplicationQueryLogic
) {
    route("/api/v1") {
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
