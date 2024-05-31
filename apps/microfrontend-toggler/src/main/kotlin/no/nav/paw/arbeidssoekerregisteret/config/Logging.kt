package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.model.NavAnsatt

// TODO Benytte audit log?
context(ConfigContext)
fun buildAuditLogMelding(
    identitetsnummer: String,
    navAnsatt: NavAnsatt,
    tilgang: String,
    melding: String,
): String =
    CefMessage.builder()
        .applicationName(appConfig.appName)
        .event(if (tilgang == "LESE") CefMessageEvent.ACCESS else CefMessageEvent.UPDATE)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.navIdent)
        .destinationUserId(identitetsnummer)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
