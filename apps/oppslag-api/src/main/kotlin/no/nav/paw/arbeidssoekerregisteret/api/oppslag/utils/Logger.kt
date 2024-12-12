package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.security.authorization.model.Action
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.buildLogger get(): Logger = LoggerFactory.getLogger(T::class.java.name)

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")
inline val buildErrorLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.error")
inline val buildAuditLogger get(): Logger = LoggerFactory.getLogger("AuditLogger")

fun Logger.audit(
    runtimeEnvironment: RuntimeEnvironment,
    aktorIdent: String,
    sluttbrukerIdent: String,
    action: Action,
    melding: String,
) {
    val message = CefMessage.builder()
        .applicationName(runtimeEnvironment.appNameOrDefaultForLocal())
        .event(action.asCefMessageEvent())
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(aktorIdent)
        .destinationUserId(sluttbrukerIdent)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
    this.info(message)
}

fun Action.asCefMessageEvent(): CefMessageEvent {
    return when (this) {
        Action.READ -> CefMessageEvent.ACCESS
        Action.WRITE -> CefMessageEvent.UPDATE
    }
}
