package no.naw.paw.minestillinger

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.naw.paw.minestillinger.api.vo.populerFylkerMedKommuner
import no.naw.paw.minestillinger.api.vo.styrkTre
import no.naw.paw.minestillinger.kodeverk.SSBKodeverk

fun Route.kodeverk() {
    route("/api/v1/kodeverk") {
        get("/fylker") {
            call.respond(populerFylkerMedKommuner(SSBKodeverk.fylker, SSBKodeverk.kommuner))
        }
        get("/styrk08") {
            call.respond(SSBKodeverk.styrkKoder.styrkTre())
        }
    }
}
