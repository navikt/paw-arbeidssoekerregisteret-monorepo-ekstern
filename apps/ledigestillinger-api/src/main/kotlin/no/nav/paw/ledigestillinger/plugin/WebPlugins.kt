package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.error.plugin.ErrorHandlingPlugin

fun Application.installWebPlugins() {
    install(IgnoreTrailingSlash)
    install(ErrorHandlingPlugin)
}
