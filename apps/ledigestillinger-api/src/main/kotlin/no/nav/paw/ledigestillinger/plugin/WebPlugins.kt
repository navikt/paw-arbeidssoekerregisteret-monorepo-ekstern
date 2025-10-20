package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash

fun Application.installWebPlugins() {
    install(IgnoreTrailingSlash)
}
