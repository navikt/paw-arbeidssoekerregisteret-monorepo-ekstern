package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.client.hentIdenter
import no.nav.paw.security.authentication.model.Identitetsnummer
import java.util.*

const val PDL_BEHANDLINGSNUMMER = "B452"

class PdlHttpConsumer(
    private val pdlClient: PdlClient,
    private val consumerId: String = currentRuntimeEnvironment.appNameOrDefaultForLocal()
) {

    suspend fun finnIdenter(identitetsnummer: Identitetsnummer): List<IdentInformasjon> {
        return pdlClient.hentIdenter(
            ident = identitetsnummer.verdi,
            callId = UUID.randomUUID().toString(),
            navConsumerId = consumerId,
            behandlingsnummer = PDL_BEHANDLINGSNUMMER,
            historikk = true
        ) ?: emptyList()
    }
}