package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.installLogginPlugin() {
    install(CallLogging) {
        level = Level.TRACE
        filter { call ->
            !call.request.path().contains("internal")
        }
    }
}