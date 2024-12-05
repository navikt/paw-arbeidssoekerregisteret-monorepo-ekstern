package no.nav.paw.security.authentication.interceptor

import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import no.nav.paw.security.authentication.model.Issuer
import no.nav.paw.security.authentication.plugin.AuthenticationPlugin

fun Route.authenticate(
    vararg issuers: Issuer = emptyArray(),
    build: Route.() -> Unit
): Route {
    install(AuthenticationPlugin)
    val configurations: Array<String> = issuers.map { issuer -> issuer.name }.toTypedArray()
    return authenticate(*configurations, optional = false, build = build)
}
