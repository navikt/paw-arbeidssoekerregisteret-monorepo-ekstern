package no.naw.paw.brukerprofiler

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.brukerprofiler.db.ops.hentBrukerProfil
import no.naw.paw.brukerprofiler.db.ops.hentProfileringOrNull
import no.naw.paw.brukerprofiler.db.ops.setKanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import java.time.Instant

class BrukerprofilTjeneste(
    private val pdlClient: PdlClient,
) {
    suspend fun kanTilbysTjenesten(identitetsnummer: Identitetsnummer): Boolean {
        val brukerProfil = hentBrukerProfil(identitetsnummer) ?: return false
        val kanTilbysTjenesten = hentCachetKanTilbysTjenesten(
            tidspunkt = Instant.now(),
            timeout = KAN_TILBYS_TJENESTEN_GYLDIGHETSPERIODE,
            kanTilbysTjenestenTimestamp = brukerProfil.kanTilbysTjenestenTimestamp,
            kanTilbysTjenesten = brukerProfil.kanTilbysTjenesten,
        )
        return when (kanTilbysTjenesten) {
            KanTilbysTjenesten.JA -> true
            KanTilbysTjenesten.NEI -> false
            KanTilbysTjenesten.UKJENT -> {
                val profilering = hentProfileringOrNull(brukerProfil.arbeidssoekerperiodeId)
                kanTilbysTjenesten(
                    brukerProfil = brukerProfil,
                    profilering = profilering,
                    harBeskyttetAdresse = { pdlClient.harBeskyttetAdresse(identitetsnummer) },
                ).also { resultat ->
                    setKanTilbysTjenesten(
                        identitetsnummer = identitetsnummer,
                        kanTilbysTjenesten = if (resultat) KanTilbysTjenesten.JA else KanTilbysTjenesten.NEI,
                        tidspunkt = Instant.now()
                    )
                }
            }
        }
    }
}