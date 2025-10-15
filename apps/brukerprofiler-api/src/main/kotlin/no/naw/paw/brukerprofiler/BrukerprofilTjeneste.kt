package no.naw.paw.brukerprofiler

import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.brukerprofiler.db.ops.hentProfileringOrNull
import no.naw.paw.brukerprofiler.db.ops.setKanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten.JA
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten.NEI
import java.time.Instant

class BrukerprofilTjeneste(
    private val pdlClient: PdlClient,
) {
    suspend fun oppdaterKanTilbysTjenesten(lagretBrukerprofiler: BrukerProfil): BrukerProfil {
        val kanTilbysTjenesten = hentCachetKanTilbysTjenesten(
            tidspunkt = Instant.now(),
            timeout = KAN_TILBYS_TJENESTEN_GYLDIGHETSPERIODE,
            kanTilbysTjenestenTimestamp = lagretBrukerprofiler.kanTilbysTjenestenTimestamp,
            kanTilbysTjenesten = lagretBrukerprofiler.kanTilbysTjenesten,
        )
        val oppdatertKanTilbysTjenesten = when (kanTilbysTjenesten) {
            KanTilbysTjenesten.UKJENT -> {
                val profilering = hentProfileringOrNull(lagretBrukerprofiler.arbeidssoekerperiodeId)
                kanTilbysTjenesten(
                    brukerProfil = lagretBrukerprofiler,
                    profilering = profilering,
                    harBeskyttetAdresse = { pdlClient.harBeskyttetAdresse(lagretBrukerprofiler.identitetsnummer) },
                ).let { resultat ->
                    setKanTilbysTjenesten(
                        identitetsnummer = lagretBrukerprofiler.identitetsnummer,
                        kanTilbysTjenesten = if (resultat) JA else NEI,
                        tidspunkt = Instant.now()
                    )
                    if (resultat) JA else NEI
                }
            }
            else -> kanTilbysTjenesten
        }
        return lagretBrukerprofiler.copy(
            kanTilbysTjenesten = oppdatertKanTilbysTjenesten
        )
    }
}