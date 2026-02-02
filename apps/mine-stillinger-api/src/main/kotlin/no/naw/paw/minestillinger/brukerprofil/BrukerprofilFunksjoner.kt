package no.naw.paw.minestillinger.brukerprofil

import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.AdressebeskyttelseMåSjekkes
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.Ja
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.Nei
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.erFremdelesGyldig
import no.naw.paw.minestillinger.domain.BrukerProfil
import java.time.Duration
import java.time.Instant

enum class TjenestenKanAktiveresResultat {
    Ja,
    Nei,
    AdressebeskyttelseMåSjekkes
}

fun BrukerProfil.tjenestenKanAktiveres(
    tidspunkt: Instant,
    adressebeskyttelseGyldighet: Duration
): TjenestenKanAktiveresResultat {
    val harGradertAdresse = flagg<HarBeskyttetadresseFlagg>()
    return when {
        harGradertAdresse == null -> AdressebeskyttelseMåSjekkes
        harGradertAdresse.erFremdelesGyldig(
            tidspunkt = tidspunkt,
            gydlighetsperiode = adressebeskyttelseGyldighet
        ) -> if (harGradertAdresse.verdi) Nei else Ja
        else -> AdressebeskyttelseMåSjekkes
    }
}
