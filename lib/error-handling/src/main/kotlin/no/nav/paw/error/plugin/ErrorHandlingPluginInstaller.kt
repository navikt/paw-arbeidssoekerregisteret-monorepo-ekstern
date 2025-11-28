package no.nav.paw.error.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.Route
import no.nav.paw.error.handler.handleException
import no.nav.paw.error.model.ProblemDetails

fun Application.installErrorHandlingPlugin(
    customResolver: (throwable: Throwable, request: ApplicationRequest) -> ProblemDetails? = { _, _ -> null }
) {
    install(ErrorHandlingPlugin) {
        this.customResolver = customResolver
    }
}

fun Route.installErrorHandlingPlugin(
    customResolver: (throwable: Throwable, request: ApplicationRequest) -> ProblemDetails? = { _, _ -> null }
) {
    install(RouteScopedErrorHandlingPlugin) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.handleException(cause, customResolver)
        }
    }
}
