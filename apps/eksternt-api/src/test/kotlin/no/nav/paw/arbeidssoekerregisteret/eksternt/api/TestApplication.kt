package no.nav.paw.arbeidssoekerregisteret.eksternt.api

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.plugins.configureSerialization
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes.arbeidssoekerperiodeRoutes
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.services.ArbeidssoekerService

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.testModule(arbeidssoekerService: ArbeidssoekerService = mockk(relaxed = true)) {
    configureSerialization()
    configureHTTP()
    routing {
        arbeidssoekerperiodeRoutes(arbeidssoekerService)
    }
}
