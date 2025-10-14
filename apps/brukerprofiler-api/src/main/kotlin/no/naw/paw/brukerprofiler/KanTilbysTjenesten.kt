package no.naw.paw.brukerprofiler

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.KanTilbysTjenesten
import no.naw.paw.brukerprofiler.domain.Profilering
import no.naw.paw.brukerprofiler.domain.ProfileringResultat.ANTATT_GODE_MULIGHETER
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofDays
import java.time.Instant

val KAN_TILBYS_TJENESTEN_GYLDIGHETSPERIODE: Duration = ofDays(1)

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

