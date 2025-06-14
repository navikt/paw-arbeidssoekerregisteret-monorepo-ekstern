package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.routes.metricsRoutes
import no.nav.paw.arbeidssoekerregisteret.routes.swaggerRoutes
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            get("/internal/isAlive") {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) { HealthStatus.HEALTHY.value }
            }

            get("/internal/isReady") {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.OK) { HealthStatus.HEALTHY.value }
            }
            metricsRoutes(prometheusMeterRegistry)
            swaggerRoutes()
            egenvurderingRoutes()
        }
    }
}

val logger = buildApplicationLogger

fun Route.egenvurderingRoutes() =
    route("/api/v1/arbeidssoeker/profilering/egenvurdering") {
        autentisering(TokenX) {
            post<EgenvurderingRequest> { egenvurderingRequest ->
                logger.info("Mottok egenvurderingrequest")
                call.respond(HttpStatusCode.Accepted)
            }
            get("/grunnlag") {
                val bruker = call.bruker<Sluttbruker>()
                logger.info("Mottok grunnlagrequest")
                call.respond(HttpStatusCode.OK, EgenvurderingGrunnlag(grunnlag = null))
            }
        }
    }


