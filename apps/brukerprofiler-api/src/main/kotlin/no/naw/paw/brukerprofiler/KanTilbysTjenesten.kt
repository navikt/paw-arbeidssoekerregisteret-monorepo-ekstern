package no.naw.paw.brukerprofiler

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.brukerprofiler.db.ops.hentBrukerProfil
import no.naw.paw.brukerprofiler.db.ops.hentProfileringOrNull
import no.naw.paw.brukerprofiler.db.ops.settKanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.Profilering
import no.naw.paw.brukerprofiler.domain.ProfileringResultat.ANTATT_GODE_MULIGHETER
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofDays
import java.time.Instant

val KAN_TILBYS_TJENESTEN_GYLDIGHETSPERIODE = ofDays(1)

class BrukerprofilTjeneste(
    private val pdlClient: PdlClient,
) {

    suspend fun kanTilbysTjenesten(
        identitetsnummer: Identitetsnummer
    ): Boolean {
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
                    settKanTilbysTjenesten(
                        identitetsnummer = identitetsnummer,
                        kanTilbysTjenesten = if (resultat) KanTilbysTjenesten.JA else KanTilbysTjenesten.NEI,
                        tidspunkt = Instant.now()
                    )
                }
            }
        }
    }
}

fun hentCachetKanTilbysTjenesten(
    tidspunkt: Instant,
    timeout: Duration,
    kanTilbysTjenestenTimestamp: Instant,
    kanTilbysTjenesten: KanTilbysTjenesten,
): KanTilbysTjenesten {
    val erUtdatert = between(kanTilbysTjenestenTimestamp, tidspunkt) > timeout
    return if (erUtdatert) {
        KanTilbysTjenesten.UKJENT
    } else {
        kanTilbysTjenesten
    }
}

suspend fun kanTilbysTjenesten(
    brukerProfil: BrukerProfil?,
    profilering: Profilering?,
    harBeskyttetAdresse: suspend (Identitetsnummer) -> Boolean,
): Boolean {
    if (brukerProfil == null) return false
    if (!sjekkABTestingGruppe(brukerProfil.identitetsnummer)) return false
    val harBruktTjenesten = brukerProfil.harBruktTjenesten
    val harGodeMuligheter = profilering?.profileringResultat == ANTATT_GODE_MULIGHETER
    return when {
        !harBruktTjenesten && !harGodeMuligheter -> false
        else -> !harBeskyttetAdresse(brukerProfil.identitetsnummer)
    }
}

fun sjekkABTestingGruppe(identitetsnummer: Identitetsnummer): Boolean {
    val andreSiffer = identitetsnummer.verdi[1].digitToInt()
    return andreSiffer % 2 == 0
}

