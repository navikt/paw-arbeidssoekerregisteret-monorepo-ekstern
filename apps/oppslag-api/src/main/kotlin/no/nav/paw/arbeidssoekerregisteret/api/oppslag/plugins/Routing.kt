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
import no.nav.paw.health.route.healthRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext.prometheusMeterRegistry)
        swaggerRoutes()
        perioderRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService
        )
        opplysningerRoutes(
            applicationContext.authorizationService,
            applicationContext.opplysningerService
        )
        profileringRoutes(
            applicationContext.authorizationService,
            applicationContext.profileringService,
            applicationContext.egenvurderingService
        )
        samletInformasjonRoutes(
            applicationContext.authorizationService,
            applicationContext.periodeService,
            applicationContext.opplysningerService,
            applicationContext.profileringService,
            applicationContext.bekreftelseService
        )
        bekreftelseRoutes(
            applicationContext.authorizationService,
            applicationContext.bekreftelseService
        )
    }
}
