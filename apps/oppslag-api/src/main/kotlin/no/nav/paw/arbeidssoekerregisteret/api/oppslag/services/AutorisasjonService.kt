package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.NavAnsatt
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.audit
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildAuditLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.poao_tilgang.client.NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangCachedClient
) {
    fun verifiserTilgangTilBruker(
        navAnsatt: NavAnsatt,
        identitetsnummer: Identitetsnummer
    ): Boolean {
        val harNavAnsattTilgang =
            poaoTilgangHttpClient.evaluatePolicy(
                NavAnsattNavIdentLesetilgangTilEksternBrukerPolicyInput(navAnsatt.navIdent, identitetsnummer.verdi)
            ).getOrThrow().isPermit

        if (!harNavAnsattTilgang) {
            buildLogger.info("NAV-ansatt har ikke tilgang til bruker")
        } else {
            buildAuditLogger.audit(identitetsnummer, navAnsatt, "NAV ansatt har hentet informasjon om bruker")
        }
        return harNavAnsattTilgang
    }
}
