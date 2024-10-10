package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.buildLogger get(): Logger = LoggerFactory.getLogger(T::class.java.name)

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")
inline val buildErrorLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.error")
inline val buildAuditLogger get(): Logger = LoggerFactory.getLogger("AuditLogger")

fun Logger.audit(
    identitetsnummer: Identitetsnummer,
    navAnsatt: NavAnsatt,
    melding: String
) {
    val message = CefMessage.builder()
        .applicationName("paw-arbeidssoekerregisteret-api-oppslag")
        .event(CefMessageEvent.ACCESS)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.navIdent)
        .destinationUserId(identitetsnummer.verdi)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()

    this.info(message)
}
