package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins


import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.bekreftelseRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.metricsRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.opplysningerRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.perioderRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.profileringRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.samletInformasjonRoutes
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.routes.swaggerRoutes
import no.nav.paw.health.liveness.livenessRoute
import no.nav.paw.health.readiness.readinessRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        routing {
            livenessRoute(applicationContext.healthChecks)
            readinessRoute(applicationContext.healthChecks)
            metricsRoutes(prometheusMeterRegistry)
            swaggerRoutes()
            perioderRoutes(
                authorizationService,
                periodeService
            )
            opplysningerRoutes(
                authorizationService,
                opplysningerService
            )
            profileringRoutes(
                authorizationService,
                profileringService,
            )
            samletInformasjonRoutes(
                authorizationService,
                periodeService,
                opplysningerService,
                profileringService,
                bekreftelseService
            )
            bekreftelseRoutes(
                authorizationService,
                bekreftelseService
            )
        }
    }
}
