package no.nav.paw.security.authentication.plugin

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authentication
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.resolveSecurityContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

class AuthenticationPluginConfig {
    var securityContextFunction: (ApplicationCall.() -> SecurityContext)? = null
}

val AuthenticationPlugin = createRouteScopedPlugin("AuthenticationPlugin", ::AuthenticationPluginConfig) {
    val securityContextFunction = pluginConfig.securityContextFunction ?: ApplicationCall::resolveSecurityContext

    on(AuthenticationChecked) { call ->
        logger.debug("Kjører autorisasjon")
        val securityContext = call.securityContextFunction()
        call.authentication.principal(securityContext)
    }
}