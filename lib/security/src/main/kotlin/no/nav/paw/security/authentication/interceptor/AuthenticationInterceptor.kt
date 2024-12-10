package no.nav.paw.security.authentication.interceptor

import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import no.nav.paw.security.authentication.model.Issuer
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.plugin.AuthenticationPlugin

fun Route.autentisering(
    issuer: Issuer,
    securityContextFunction: (suspend (SecurityContext) -> SecurityContext)? = null,
    build: Route.() -> Unit
): Route {
    return autentisering(issuers = arrayOf(issuer), securityContextFunction = securityContextFunction, build = build)
}

fun Route.autentisering(
    vararg issuers: Issuer = emptyArray(),
    securityContextFunction: (suspend (SecurityContext) -> SecurityContext)? = null,
    build: Route.() -> Unit
): Route {
    install(AuthenticationPlugin) {
        this.securityContextFunction = securityContextFunction
    }
    val configurations: Array<String> = issuers.map { issuer -> issuer.name }.toTypedArray()
    return authenticate(*configurations, optional = false, build = build)
}
