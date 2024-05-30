package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.config.buildAuditLogMelding
import no.nav.paw.arbeidssoekerregisteret.context.ConfigContext
import no.nav.paw.arbeidssoekerregisteret.context.LoggingContext
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.TilgangType
import java.util.*

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangCachedClient
) {

    context(ConfigContext, LoggingContext)
    fun verifiserTilgangTilBruker(
        navAnsatt: NavAnsatt,
        identitetsnummer: String,
        tilgangType: TilgangType
    ): Boolean {
        val harNavAnsattTilgang =
            poaoTilgangHttpClient.evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    navAnsattAzureId = navAnsatt.azureId,
                    tilgangType = tilgangType,
                    norskIdent = identitetsnummer
                )
            ).getOrThrow().isPermit

        if (!harNavAnsattTilgang) {
            logger.info("NAV-ansatt har ikke $tilgangType til bruker")
        } else {
            auditLogger.info(
                buildAuditLogMelding(
                    identitetsnummer,
                    navAnsatt,
                    tilgangType,
                    "NAV ansatt har benyttet $tilgangType tilgang til informasjon om bruker"
                )
            )
        }
        return harNavAnsattTilgang
    }
}

data class NavAnsatt(val azureId: UUID, val navIdent: String)