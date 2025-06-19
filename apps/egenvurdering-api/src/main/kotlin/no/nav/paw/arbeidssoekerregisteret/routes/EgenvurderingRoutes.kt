package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering

val logger = buildApplicationLogger

fun Route.egenvurderingRoutes() =
    route("/api/v1/arbeidssoeker/profilering/egenvurdering") {
        autentisering(TokenX) {
            post<EgenvurderingRequest> { egenvurderingRequest ->
                logger.info("Mottok egenvurderingrequest")
                // TODO: Ta en runde på nødvendig innhold i EgenvurderingRequest
                call.respond(HttpStatusCode.Accepted)
            }
            get("/grunnlag") {
                val bruker = call.bruker<Sluttbruker>()
                logger.info("Mottok grunnlagrequest")
                call.respond(HttpStatusCode.OK, EgenvurderingGrunnlag(grunnlag = null))
            }
        }
    }