package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.request.uri
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.client.api.oppslag.exception.ArbeidssoekerperioderAggregertOppslagResponseException
import no.nav.paw.error.model.ProblemDetailsBuilder
import no.nav.paw.error.model.asHttpErrorType
import no.nav.paw.error.plugin.ErrorHandlingPlugin

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(ErrorHandlingPlugin) {
        customResolver = { cause, request ->
            when (cause) {
                is ArbeidssoekerperioderAggregertOppslagResponseException -> {
                    ProblemDetailsBuilder.builder()
                        .type("fikk-ikke-kontakt-med-oppslag-api".asHttpErrorType())
                        .status(HttpStatusCode.BadGateway)
                        .detail("Fikk ikke kontakt med oppslag API")
                        .instance(request.uri)
                        .build()
                }

                else -> null
            }
        }
    }
    installTracingPlugin()
}
