package no.nav.paw.oppslagapi.routes.v1

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.oppslagapi.data.query.ApplicationQueryLogic
import no.nav.paw.oppslagapi.plugin.installContentNegotiation
import no.nav.paw.oppslagapi.plugin.installErrorHandler
import no.nav.paw.oppslagapi.utils.configureJacksonForV1

fun Route.v1Routes(
    queryLogic: ApplicationQueryLogic
) {
    route("/api/v1") {
        installContentNegotiation {
            configureJacksonForV1()
        }
        installErrorHandler()

        v1Perioder(queryLogic)
        v1VeilederPerioder(queryLogic)

        v1PerioderAggregert(queryLogic)
        v1VeilederPerioderAggregert(queryLogic)
        v1SamletInformasjon(queryLogic)
        v1VeilederSamletInformasjon(queryLogic)

        v1Opplysninger(queryLogic)
        v1VeilederOpplysninger(queryLogic)
        v1Profilering(queryLogic)
        v1VeilederProfilering(queryLogic)
        v1Bekrefelser(queryLogic)
        v1VeilederBekreftelser(queryLogic)
    }
}
