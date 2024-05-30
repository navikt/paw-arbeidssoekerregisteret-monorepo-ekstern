package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.service.NavAnsatt
import no.nav.poao_tilgang.client.TilgangType

context(ConfigContext)
fun buildAuditLogMelding(
    identitetsnummer: String,
    navAnsatt: NavAnsatt,
    tilgangType: TilgangType,
    melding: String,
): String =
    CefMessage.builder()
        .applicationName(appConfig.appName)
        .event(if (tilgangType == TilgangType.LESE) CefMessageEvent.ACCESS else CefMessageEvent.UPDATE)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.navIdent)
        .destinationUserId(identitetsnummer)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
