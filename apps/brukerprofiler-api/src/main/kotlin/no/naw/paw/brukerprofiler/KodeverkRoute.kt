package no.naw.paw.brukerprofiler

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.brukerprofiler.api.vo.populerFylkerMedKommuner
import no.naw.paw.brukerprofiler.kodeverk.SSBKodeverk

fun Route.kodeverk() {
    route("/kodeverk") {
        autentisering(TokenX) {
            get("/fylker") {
                call.respond(populerFylkerMedKommuner(SSBKodeverk.fylker, SSBKodeverk.kommuner))
            }
        }
    }
}