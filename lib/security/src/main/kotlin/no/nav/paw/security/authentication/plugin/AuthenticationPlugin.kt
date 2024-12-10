package no.nav.paw.security.authentication.plugin

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.resolveSecurityContext
import no.nav.paw.security.authentication.model.securityContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

class AuthenticationPluginConfig {
    var securityContextFunction: (suspend (SecurityContext) -> SecurityContext)? = null
}

val AuthenticationPlugin = createRouteScopedPlugin("AuthenticationPlugin", ::AuthenticationPluginConfig) {
    val securityContextFunction = pluginConfig.securityContextFunction ?: { it }

    on(AuthenticationChecked) { call ->
        logger.debug("Kjører autentisering")
        val securityContext = securityContextFunction(call.resolveSecurityContext())
        call.securityContext(securityContext)
    }
}